package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.entity.Appointment;
import lk.lankacare.medichannel.entity.ChatMessage;
import lk.lankacare.medichannel.entity.Consultation;
import lk.lankacare.medichannel.entity.User;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.AppointmentRepository;
import lk.lankacare.medichannel.repository.ChatMessageRepository;
import lk.lankacare.medichannel.repository.ConsultationRepository;
import lk.lankacare.medichannel.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enforces Privacy Rule (Point 2), BR-09 (online room opens only when doctor starts,
 * admits only that patient and treating doctor, read-only when ended),
 * Point 3.4 (audit log on every opening of a consultation record without medical data),
 * and RT-03 encrypted chat up to 1000 characters.
 */
@Service
public class ConsultationService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    public ConsultationService(
            AppointmentRepository appointmentRepository,
            ConsultationRepository consultationRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            AuditService auditService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Map<String, Object> openConsultationRecord(Long userId, String role, Long appointmentId, String ip) {
        Appointment appt = verifyParticipantAccess(userId, role, appointmentId);

        // Point 3.4: Audit EVERY opening of a consultation record (without storing any medical data!)
        auditService.record(
                userId,
                role,
                "OPEN_CONSULTATION_RECORD",
                "APPOINTMENT",
                String.valueOf(appointmentId),
                ip,
                "Authorized participant viewed consultation record"
        );

        Consultation c = consultationRepository.findByAppointmentId(appointmentId).orElse(null);
        List<Map<String, Object>> messages = (c != null)
                ? chatMessageRepository.findByConsultationIdOrderBySentAtAsc(c.getId()).stream()
                        .map(this::toChatDto)
                        .toList()
                : List.of();

        Map<String, Object> out = new HashMap<>();
        out.put("appointmentId", appt.getId());
        out.put("bookingReference", appt.getBookingReference());
        out.put("sessionType", appt.getSession().getSessionType());
        out.put("appointmentStatus", appt.getStatus());
        out.put("doctorName", appt.getSession().getDoctor().getUser().getFullName());
        out.put("doctorSlmc", appt.getSession().getDoctor().getSlmcRegNo());
        out.put("specialization", appt.getSession().getDoctor().getSpecialization().getName());
        out.put("patientName", appt.getPatient().getUser().getFullName());
        out.put("sessionDate", appt.getSession().getSessionDate());
        out.put("reasonForVisit", appt.getReasonForVisit());
        out.put("roomOpen", c != null && c.isRoomOpen());
        out.put("readOnly", c == null || !c.isRoomOpen());
        out.put("diagnosis", c != null ? c.getDiagnosis() : null);
        out.put("advice", c != null ? c.getAdvice() : null);
        out.put("prescription", c != null ? c.getPrescription() : null);
        out.put("startedAt", c != null ? c.getStartedAt() : null);
        out.put("endedAt", c != null ? c.getEndedAt() : null);
        out.put("messages", messages);
        return out;
    }

    @Transactional
    public Map<String, Object> saveNotesAndEndConsultation(
            Long doctorUserId,
            Long appointmentId,
            BookingDtos.ConsultationNotesRequest req,
            boolean endConsultation
    ) {
        Appointment appt = verifyParticipantAccess(doctorUserId, "DOCTOR", appointmentId);

        Consultation c = consultationRepository.findByAppointmentId(appointmentId).orElseGet(() -> {
            Consultation created = new Consultation();
            created.setAppointment(appt);
            created.setStartedAt(Instant.now());
            return created;
        });

        c.setDiagnosis(req.getDiagnosis().trim());
        c.setAdvice(req.getAdvice() != null ? req.getAdvice().trim() : "");
        c.setPrescription(req.getPrescription() != null ? req.getPrescription().trim() : "");

        if (endConsultation) {
            c.setRoomOpen(false);
            c.setEndedAt(Instant.now());
            appt.setStatus("COMPLETED");
            appointmentRepository.save(appt);
            messagingTemplate.convertAndSend(
                    "/topic/consultations/" + appointmentId + "/status",
                    Map.of("roomOpen", false, "status", "COMPLETED")
            );
        }
        consultationRepository.save(c);

        Map<String, Object> out = new HashMap<>();
        out.put("appointmentId", appt.getId());
        out.put("roomOpen", c.isRoomOpen());
        out.put("appointmentStatus", appt.getStatus());
        out.put("diagnosis", c.getDiagnosis());
        out.put("advice", c.getAdvice());
        out.put("prescription", c.getPrescription());
        return out;
    }

    @Transactional
    public Map<String, Object> sendChatMessage(Long userId, String role, Long appointmentId, String messageText) {
        if (messageText == null || messageText.trim().isEmpty() || messageText.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chat message must be between 1 and 1000 characters (Point 5).");
        }
        verifyParticipantAccess(userId, role, appointmentId);

        Consultation c = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Consultation room has not been started yet."));

        if (!c.isRoomOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Consultation has ended. Chat is now read-only.");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        ChatMessage msg = new ChatMessage();
        msg.setConsultation(c);
        msg.setSender(sender);
        msg.setSenderRole(role);
        msg.setMessageText(messageText.trim()); // Automatically AES-256-GCM encrypted at rest
        msg = chatMessageRepository.save(msg);

        Map<String, Object> dto = toChatDto(msg);
        messagingTemplate.convertAndSend("/topic/consultations/" + appointmentId + "/chat", dto);
        return dto;
    }

    private Appointment verifyParticipantAccess(Long userId, String role, Long appointmentId) {
        // Strict Privacy Rule: ADMIN and MANAGER are NEVER allowed to view medical data
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Privacy rule: ADMIN and MANAGER cannot access medical consultations.");
        }
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));

        boolean isPatient = "CUSTOMER".equals(role) && appt.getPatient().getUser().getId().equals(userId);
        boolean isDoctor = "DOCTOR".equals(role) && appt.getSession().getDoctor().getUser().getId().equals(userId);
        if (!isPatient && !isDoctor) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to access this consultation.");
        }
        return appt;
    }

    private Map<String, Object> toChatDto(ChatMessage m) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", m.getId());
        dto.put("senderUserId", m.getSender().getId());
        dto.put("senderName", m.getSender().getFullName());
        dto.put("senderRole", m.getSenderRole());
        dto.put("messageText", m.getMessageText());
        dto.put("sentAt", m.getSentAt());
        return dto;
    }
}
