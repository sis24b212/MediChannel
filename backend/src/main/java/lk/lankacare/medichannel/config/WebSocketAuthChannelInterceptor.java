package lk.lankacare.medichannel.config;

import io.jsonwebtoken.Claims;
import lk.lankacare.medichannel.entity.Appointment;
import lk.lankacare.medichannel.entity.User;
import lk.lankacare.medichannel.repository.AppointmentRepository;
import lk.lankacare.medichannel.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Enforces Point 5 WebSocket security:
 * - Authenticates CONNECT frames using the signed JWT token (guests may only view public availability).
 * - Checks role & ownership on every SUBSCRIBE and SEND frame, especially private
 *   consultation chat rooms (BR-09 & Privacy Rule: ADMIN and MANAGER strictly blocked).
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public WebSocketAuthChannelInterceptor(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    Long userId = jwtUtil.extractUserId(claims);
                    String role = jwtUtil.extractRole(claims);
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent() && userOpt.get().isActive()) {
                        UsernamePasswordAuthenticationToken principal =
                                new UsernamePasswordAuthenticationToken(
                                        String.valueOf(userId),
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                );
                        accessor.setUser(principal);
                    }
                } catch (Exception ex) {
                    throw new MessagingException("Invalid or expired WebSocket authentication token.");
                }
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return message;
            }

            // Public availability & live queue broadcast topics are viewable by anyone viewing a session
            if (destination.startsWith("/topic/sessions/") && (destination.endsWith("/availability") || destination.endsWith("/queue"))) {
                return message;
            }

            // All other destinations require an authenticated principal
            Principal user = accessor.getUser();
            if (!(user instanceof UsernamePasswordAuthenticationToken auth)) {
                throw new MessagingException("Authentication required for destination: " + destination);
            }

            Long userId = Long.parseLong(auth.getName());
            String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

            // Private consultation room chat & typing topics: ONLY the treating DOCTOR and that PATIENT
            if (destination.startsWith("/topic/consultations/") || destination.startsWith("/app/consultations/")) {
                if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                    throw new MessagingException("Privacy violation: ADMIN and MANAGER cannot access consultation chats.");
                }
                Long appointmentId = extractIdAfterPrefix(destination, "consultations/");
                Appointment appt = appointmentRepository.findById(appointmentId)
                        .orElseThrow(() -> new MessagingException("Appointment not found."));

                boolean isPatient = appt.getPatient().getUser().getId().equals(userId);
                boolean isDoctor = appt.getSession().getDoctor().getUser().getId().equals(userId);
                if (!isPatient && !isDoctor) {
                    throw new MessagingException("Forbidden: you are not a participant of this consultation.");
                }
            }
        }

        return message;
    }

    private Long extractIdAfterPrefix(String destination, String marker) {
        int idx = destination.indexOf(marker);
        if (idx < 0) {
            throw new MessagingException("Invalid consultation destination.");
        }
        String remainder = destination.substring(idx + marker.length());
        String idPart = remainder.split("/")[0];
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException ex) {
            throw new MessagingException("Invalid appointment ID in WebSocket destination.");
        }
    }
}
