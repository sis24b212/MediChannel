package lk.lankacare.medichannel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.service.ConsultationService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Private consultation endpoints accessible ONLY to CUSTOMER (own appointment)
 * and DOCTOR (treating doctor). ADMIN and MANAGER receive 403 Forbidden.
 */
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Map<String, Object>> getConsultation(
            Authentication auth,
            @PathVariable Long appointmentId,
            HttpServletRequest httpReq
    ) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(consultationService.openConsultationRecord(userId, role, appointmentId, httpReq.getRemoteAddr()));
    }

    @PutMapping("/{appointmentId}/notes")
    public ResponseEntity<Map<String, Object>> saveNotes(
            Authentication auth,
            @PathVariable Long appointmentId,
            @RequestParam(defaultValue = "false") boolean endConsultation,
            @Valid @RequestBody BookingDtos.ConsultationNotesRequest req
    ) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(consultationService.saveNotesAndEndConsultation(userId, appointmentId, req, endConsultation));
    }

    @PostMapping("/{appointmentId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessageRest(
            Authentication auth,
            @PathVariable Long appointmentId,
            @Valid @RequestBody BookingDtos.ChatMessagePayload payload
    ) {
        Long userId = (Long) auth.getPrincipal();
        String role = extractRole(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(consultationService.sendChatMessage(userId, role, appointmentId, payload.getMessageText()));
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
