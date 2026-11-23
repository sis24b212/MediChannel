package lk.lankacare.medichannel.controller;

import jakarta.validation.Valid;
import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.entity.Patient;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.PatientRepository;
import lk.lankacare.medichannel.service.AppointmentService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    public PatientController(AppointmentService appointmentService, PatientRepository patientRepository) {
        this.appointmentService = appointmentService;
        this.patientRepository = patientRepository;
    }

    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> bookAppointment(
            Authentication auth,
            @Valid @RequestBody BookingDtos.CreateAppointmentRequest req
    ) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(appointmentService.bookForPatient(userId, req));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> getMyAppointments(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(appointmentService.getMyAppointments(userId));
    }

    @PatchMapping("/appointments/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelAppointment(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(appointmentService.cancelMyAppointment(userId, id));
    }

    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            Authentication auth,
            @Valid @RequestBody BookingDtos.FeedbackRequest req
    ) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(appointmentService.submitFeedback(userId, req));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Patient p = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient profile not found."));
        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", p.getUser().getFullName());
        profile.put("email", p.getUser().getEmail());
        profile.put("nic", p.getNic());
        profile.put("dateOfBirth", p.getDateOfBirth());
        profile.put("gender", p.getGender());
        profile.put("phone", p.getPhone());
        profile.put("address", p.getAddress());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            Authentication auth,
            @RequestBody Map<String, String> payload
    ) {
        Long userId = (Long) auth.getPrincipal();
        Patient p = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient profile not found."));

        if (payload.containsKey("phone")) {
            String phone = payload.get("phone").trim();
            if (!phone.matches("^07\\d{8}$")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Phone must follow format 07XXXXXXXX (BR-10).");
            }
            p.setPhone(phone);
        }
        if (payload.containsKey("address") && !payload.get("address").isBlank()) {
            p.setAddress(payload.get("address").trim());
        }
        patientRepository.save(p);
        return getProfile(auth);
    }
}
