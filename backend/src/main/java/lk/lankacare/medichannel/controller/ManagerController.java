package lk.lankacare.medichannel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.entity.Doctor;
import lk.lankacare.medichannel.entity.Notice;
import lk.lankacare.medichannel.entity.Room;
import lk.lankacare.medichannel.entity.Specialization;
import lk.lankacare.medichannel.repository.FeedbackRepository;
import lk.lankacare.medichannel.repository.NoticeRepository;
import lk.lankacare.medichannel.repository.RoomRepository;
import lk.lankacare.medichannel.service.AppointmentService;
import lk.lankacare.medichannel.service.ManagerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ManagerService managerService;
    private final AppointmentService appointmentService;
    private final RoomRepository roomRepository;
    private final NoticeRepository noticeRepository;
    private final FeedbackRepository feedbackRepository;

    public ManagerController(
            ManagerService managerService,
            AppointmentService appointmentService,
            RoomRepository roomRepository,
            NoticeRepository noticeRepository,
            FeedbackRepository feedbackRepository
    ) {
        this.managerService = managerService;
        this.appointmentService = appointmentService;
        this.roomRepository = roomRepository;
        this.noticeRepository = noticeRepository;
        this.feedbackRepository = feedbackRepository;
    }

    // ---- Catalog Management ----

    @PostMapping("/specializations")
    public ResponseEntity<Specialization> createSpecialization(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managerService.createSpecialization(body.getOrDefault("name", ""), body.get("description")));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> listRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managerService.createRoom(body.getOrDefault("roomNumber", ""), body.getOrDefault("floor", ""), body.get("wing")));
    }

    @PostMapping("/doctors")
    public ResponseEntity<Doctor> saveDoctorProfile(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managerService.saveDoctorProfile(
                        Long.valueOf(body.get("userId").toString()),
                        String.valueOf(body.get("slmcRegNo")),
                        Long.valueOf(body.get("specializationId").toString()),
                        String.valueOf(body.get("qualifications")),
                        new BigDecimal(body.get("consultationFee").toString()),
                        body.get("bio") != null ? String.valueOf(body.get("bio")) : ""
                ));
    }

    // ---- Session Management ----

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@Valid @RequestBody BookingDtos.SessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createSession(req));
    }

    @PostMapping("/sessions/{id}/delay")
    public ResponseEntity<Map<String, Object>> delaySession(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            HttpServletRequest httpReq
    ) {
        Long managerId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                managerService.delaySession(managerId, id, body.getOrDefault("delayMinutes", 15), httpReq.getRemoteAddr())
        );
    }

    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSession(
            Authentication auth,
            @PathVariable Long id,
            HttpServletRequest httpReq
    ) {
        Long managerId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(managerService.cancelSession(managerId, id, httpReq.getRemoteAddr()));
    }

    // ---- Reception ----

    @GetMapping("/reception/appointments")
    public ResponseEntity<Page<Map<String, Object>>> receptionAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(managerService.getReceptionAppointments(date, page, size));
    }

    @PatchMapping("/reception/appointments/{id}/arrive")
    public ResponseEntity<Map<String, Object>> markArrived(@PathVariable Long id) {
        return ResponseEntity.ok(managerService.markArrived(id));
    }

    @PatchMapping("/reception/appointments/{id}/payment")
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody BookingDtos.PaymentRequest req
    ) {
        return ResponseEntity.ok(managerService.recordPayment(id, req));
    }

    @PostMapping("/reception/walk-in")
    public ResponseEntity<Map<String, Object>> bookWalkIn(@Valid @RequestBody BookingDtos.WalkInBookingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookWalkIn(req));
    }

    // ---- Notices & Feedback Moderation ----

    @GetMapping("/notices")
    public ResponseEntity<List<Notice>> allNotices() {
        return ResponseEntity.ok(noticeRepository.findAllByOrderByPublishedAtDesc());
    }

    @PostMapping("/notices")
    public ResponseEntity<Notice> publishNotice(Authentication auth, @RequestBody Map<String, String> body) {
        Long managerId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managerService.createNotice(managerId, body.getOrDefault("title", ""), body.getOrDefault("content", ""), body.get("priority")));
    }

    @GetMapping("/feedback")
    public ResponseEntity<?> listFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                feedbackRepository.findByModerationStatusNotOrderByCreatedAtDesc("DELETED", PageRequest.of(page, size))
        );
    }

    @PatchMapping("/feedback/{id}")
    public ResponseEntity<Map<String, Object>> moderateFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(managerService.moderateFeedback(id, body.getOrDefault("moderationStatus", "APPROVED")));
    }

    // ---- Dashboard & SQL/JPA Reports ----

    @GetMapping("/reports/dashboard")
    public ResponseEntity<Map<String, Object>> dashboardReports() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(managerService.getDashboardReports());
    }
}
