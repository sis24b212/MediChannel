package lk.lankacare.medichannel.controller;

import lk.lankacare.medichannel.service.ConsultationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP WebSocket controller for RT-03 real-time encrypted doctor-patient chat
 * and typing indicators. Permissions are verified both in WebSocketAuthChannelInterceptor
 * and in ConsultationService.
 */
@Controller
public class WebSocketChatController {

    private final ConsultationService consultationService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(ConsultationService consultationService, SimpMessagingTemplate messagingTemplate) {
        this.consultationService = consultationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/consultations/{appointmentId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long appointmentId,
            @Payload Map<String, String> payload,
            Principal principal
    ) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        Long userId = Long.parseLong(auth.getName());
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String text = payload.getOrDefault("messageText", "");
        consultationService.sendChatMessage(userId, role, appointmentId, text);
    }

    @MessageMapping("/consultations/{appointmentId}/typing")
    public void handleTyping(
            @DestinationVariable Long appointmentId,
            @Payload Map<String, Object> payload,
            Principal principal
    ) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        boolean typing = Boolean.TRUE.equals(payload.get("typing"));
        messagingTemplate.convertAndSend(
                "/topic/consultations/" + appointmentId + "/typing",
                Map.of("senderRole", role, "typing", typing)
        );
    }
}
