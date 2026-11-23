package lk.lankacare.medichannel.controller;

import lk.lankacare.medichannel.service.DoctorSessionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorSessionService doctorSessionService;

    public DoctorController(DoctorSessionService doctorSessionService) {
        this.doctorSessionService = doctorSessionService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> mySessions(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(doctorSessionService.getMySessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/patients")
    public ResponseEntity<List<Map<String, Object>>> sessionPatients(
            Authentication auth,
            @PathVariable Long sessionId
    ) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(doctorSessionService.getSessionPatients(userId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/start")
    public ResponseEntity<Map<String, Object>> startSession(Authentication auth, @PathVariable Long sessionId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(doctorSessionService.startSession(userId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/call-next")
    public ResponseEntity<Map<String, Object>> callNext(Authentication auth, @PathVariable Long sessionId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(doctorSessionService.callNextPatient(userId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(Authentication auth, @PathVariable Long sessionId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(doctorSessionService.endSession(userId, sessionId));
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    public ResponseEntity<Map<String, Object>> updateAppointmentStatus(
            Authentication auth,
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> payload
    ) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                doctorSessionService.markAppointmentStatus(userId, appointmentId, payload.getOrDefault("status", ""))
        );
    }
}
