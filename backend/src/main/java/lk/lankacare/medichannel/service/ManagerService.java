package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.config.CacheConfig;
import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.entity.*;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Manager operations (Point 3.3):
 * - Specializations, rooms, doctor profiles (invalidates public cache)
 * - Sessions (create, edit, delay recalculating BR-06 times, cancel notifying all patients)
 * - Reception (today's appointments WITHOUT medical data, ARRIVED check-in, payment CASH/CARD)
 * - Notices & Feedback moderation
 * - Dashboard & SQL/JPA reports
 */
@Service
public class ManagerService {

    private final SpecializationRepository specializationRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ChannelSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final FeedbackRepository feedbackRepository;
    private final NoticeRepository noticeRepository;
    private final RealtimeEventService realtimeEventService;
    private final AuditService auditService;
    private final PublicCatalogService publicCatalogService;

    public ManagerService(
            SpecializationRepository specializationRepository,
            RoomRepository roomRepository,
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            ChannelSessionRepository sessionRepository,
            AppointmentRepository appointmentRepository,
            FeedbackRepository feedbackRepository,
            NoticeRepository noticeRepository,
            RealtimeEventService realtimeEventService,
            AuditService auditService,
            PublicCatalogService publicCatalogService
    ) {
        this.specializationRepository = specializationRepository;
        this.roomRepository = roomRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
        this.feedbackRepository = feedbackRepository;
        this.noticeRepository = noticeRepository;
        this.realtimeEventService = realtimeEventService;
        this.auditService = auditService;
        this.publicCatalogService = publicCatalogService;
    }

    @CacheEvict(value = CacheConfig.SPECIALIZATIONS_CACHE, allEntries = true)
    public Specialization createSpecialization(String name, String description) {
        if (specializationRepository.existsByNameIgnoreCase(name.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Specialization already exists.");
        }
        Specialization s = new Specialization();
        s.setName(name.trim());
        s.setDescription(description);
        return specializationRepository.save(s);
    }

    public Room createRoom(String roomNumber, String floor, String wing) {
        if (roomRepository.existsByRoomNumberIgnoreCase(roomNumber.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Room number already exists.");
        }
        Room r = new Room();
        r.setRoomNumber(roomNumber.trim());
        r.setFloor(floor.trim());
        r.setWing(wing);
        return roomRepository.save(r);
    }

    @CacheEvict(value = CacheConfig.DOCTORS_CACHE, allEntries = true)
    @Transactional
    public Doctor saveDoctorProfile(Long userId, String slmcRegNo, Long specializationId, String qualifications, BigDecimal fee, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor user account not found."));
        if (!"DOCTOR".equals(user.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected user does not have the DOCTOR role.");
        }
        Specialization spec = specializationRepository.findById(specializationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Specialization not found."));

        Doctor doctor = doctorRepository.findByUserId(userId).orElseGet(Doctor::new);
        doctor.setUser(user);
        doctor.setSlmcRegNo(slmcRegNo.trim().toUpperCase());
        doctor.setSpecialization(spec);
        doctor.setQualifications(qualifications.trim());
        doctor.setConsultationFee(fee);
        doctor.setBio(bio);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Map<String, Object> createSession(BookingDtos.SessionRequest req) {
        validateSessionRules(req, null);

        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor not found."));
        Room room = null;
        if ("PHYSICAL".equals(req.getSessionType())) {
            room = roomRepository.findById(req.getRoomId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found."));
        }

        ChannelSession session = new ChannelSession();
        session.setDoctor(doctor);
        session.setRoom(room);
        session.setSessionDate(req.getSessionDate());
        session.setStartTime(req.getStartTime());
        session.setEndTime(req.getEndTime());
        session.setSessionType(req.getSessionType());
        session.setMaxPatients(req.getMaxPatients());
        session.setMinutesPerPatient(req.getMinutesPerPatient());
        session.setFeeLkr(req.getFeeLkr());
        session.setStatus("OPEN");
        session = sessionRepository.save(session);
        return publicCatalogService.toSessionDto(session);
    }

    @Transactional
    public Map<String, Object> delaySession(Long managerUserId, Long sessionId, int extraDelayMinutes, String ip) {
        if (extraDelayMinutes <= 0 || extraDelayMinutes > 240) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Delay minutes must be between 1 and 240.");
        }
        ChannelSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));
        if ("CANCELLED".equals(session.getStatus()) || "COMPLETED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delay a cancelled or completed session.");
        }

        session.setDelayMinutes(session.getDelayMinutes() + extraDelayMinutes);
        sessionRepository.save(session);

        // Recalculate estimated times (BR-06) and notify every active patient live (RT-04)
        List<Appointment> appts = appointmentRepository.findBySessionIdOrderByAppointmentNumberAsc(sessionId);
        for (Appointment a : appts) {
            if (!"CANCELLED".equals(a.getStatus()) && !"COMPLETED".equals(a.getStatus())) {
                LocalTime updatedEst = session.getStartTime()
                        .plusMinutes((long) (a.getAppointmentNumber() - 1) * session.getMinutesPerPatient() + session.getDelayMinutes());
                a.setEstimatedTime(updatedEst);
                appointmentRepository.save(a);

                realtimeEventService.sendUserNotification(
                        a.getPatient().getUser(),
                        "SESSION_DELAYED",
                        "Session Delayed by " + extraDelayMinutes + " Minutes",
                        session.getDoctor().getUser().getFullName() + "'s session on " + session.getSessionDate()
                                + " is delayed. Your updated estimated time is " + updatedEst + "."
                );
            }
        }

        realtimeEventService.broadcastQueueUpdate(session.getId(), session.getCurrentServingNumber(), session.getStatus(), session.getDelayMinutes());
        auditService.record(managerUserId, "MANAGER", "SESSION_DELAYED", "SESSION", String.valueOf(sessionId), ip, "Delayed by " + extraDelayMinutes + " minutes");
        return publicCatalogService.toSessionDto(session);
    }

    @Transactional
    public Map<String, Object> cancelSession(Long managerUserId, Long sessionId, String ip) {
        ChannelSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));
        if ("COMPLETED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed sessions cannot be cancelled.");
        }

        session.setStatus("CANCELLED");
        sessionRepository.save(session);

        // Cancel every appointment in the session (BR-11 keep records, never delete) & notify live
        List<Appointment> appts = appointmentRepository.findBySessionIdOrderByAppointmentNumberAsc(sessionId);
        for (Appointment a : appts) {
            if (!"CANCELLED".equals(a.getStatus())) {
                a.setStatus("CANCELLED");
                a.setCancelledAt(Instant.now());
                appointmentRepository.save(a);

                realtimeEventService.sendUserNotification(
                        a.getPatient().getUser(),
                        "SESSION_CANCELLED",
                        "Channelling Session Cancelled",
                        "We apologize — " + session.getDoctor().getUser().getFullName() + "'s session on "
                                + session.getSessionDate() + " has been cancelled. Please rebook an alternate date."
                );
            }
        }

        realtimeEventService.broadcastQueueUpdate(session.getId(), session.getCurrentServingNumber(), "CANCELLED", session.getDelayMinutes());
        auditService.record(managerUserId, "MANAGER", "SESSION_CANCELLED", "SESSION", String.valueOf(sessionId), ip, "Session and all appointments cancelled");
        return publicCatalogService.toSessionDto(session);
    }

    // Reception: today's appointments (STRICTLY excludes reasonForVisit / medical notes!)
    public Page<Map<String, Object>> getReceptionAppointments(LocalDate date, int page, int size) {
        LocalDate target = date != null ? date : LocalDate.now();
        return appointmentRepository
                .findBySessionSessionDateOrderBySessionStartTimeAscAppointmentNumberAsc(target, PageRequest.of(page, size))
                .map(this::toReceptionDto);
    }

    @Transactional
    public Map<String, Object> markArrived(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));
        if (!"PHYSICAL".equals(appt.getSession().getSessionType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only physical appointments require reception check-in.");
        }
        if (!"BOOKED".equals(appt.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Appointment must be in BOOKED status to mark ARRIVED.");
        }
        appt.setStatus("ARRIVED");
        appointmentRepository.save(appt);
        return toReceptionDto(appt);
    }

    @Transactional
    public Map<String, Object> recordPayment(Long appointmentId, BookingDtos.PaymentRequest req) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));
        appt.setPaymentStatus(req.getPaymentStatus());
        appt.setPaymentMethod("PAID".equals(req.getPaymentStatus()) ? req.getPaymentMethod() : null);
        appointmentRepository.save(appt);
        return toReceptionDto(appt);
    }

    public Notice createNotice(Long managerUserId, String title, String content, String priority) {
        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manager account not found."));
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setContent(content.trim());
        n.setPriority(priority != null ? priority.toUpperCase() : "NORMAL");
        n.setCreatedBy(manager);
        return noticeRepository.save(n);
    }

    @Transactional
    public Map<String, Object> moderateFeedback(Long feedbackId, String moderationStatus) {
        if (!List.of("APPROVED", "HIDDEN", "DELETED").contains(moderationStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status must be APPROVED, HIDDEN, or DELETED.");
        }
        Feedback fb = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feedback not found."));
        fb.setModerationStatus(moderationStatus);
        feedbackRepository.save(fb);
        return Map.of("id", fb.getId(), "moderationStatus", fb.getModerationStatus());
    }

    // Backend SQL/JPQL Dashboard & Reports (Point 3.3)
    public Map<String, Object> getDashboardReports() {
        List<Map<String, Object>> todaySessions = sessionRepository.findBySessionDateOrderByStartTimeAsc(LocalDate.now())
                .stream()
                .map(publicCatalogService::toSessionDto)
                .toList();

        List<Map<String, Object>> appointmentsPerDay = appointmentRepository.countAppointmentsPerDay().stream()
                .map(r -> Map.<String, Object>of("date", r[0].toString(), "count", r[1]))
                .toList();

        Map<String, Long> physicalVsOnline = new HashMap<>();
        physicalVsOnline.put("PHYSICAL", 0L);
        physicalVsOnline.put("ONLINE", 0L);
        for (Object[] row : appointmentRepository.countPhysicalVsOnline()) {
            physicalVsOnline.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        long noShows = appointmentRepository.countNoShows();
        long finished = appointmentRepository.countFinishedAppointments();
        double noShowRate = finished > 0 ? Math.round((noShows * 10000.0) / finished) / 100.0 : 0.0;

        BigDecimal totalRevenue = appointmentRepository.sumPaidRevenue();

        Map<Long, String> doctorNames = new HashMap<>();
        doctorRepository.findAll().forEach(d -> doctorNames.put(d.getId(), d.getUser().getFullName()));

        List<Map<String, Object>> topDoctors = appointmentRepository.findTopDoctorsByAppointmentCount().stream()
                .limit(5)
                .map(r -> Map.<String, Object>of(
                        "doctorId", r[0],
                        "doctorName", doctorNames.getOrDefault(((Number) r[0]).longValue(), "Unknown"),
                        "appointmentCount", r[1]
                ))
                .toList();

        List<Map<String, Object>> averageRatings = feedbackRepository.averageRatingPerDoctor().stream()
                .map(r -> Map.<String, Object>of(
                        "doctorId", r[0],
                        "doctorName", doctorNames.getOrDefault(((Number) r[0]).longValue(), "Unknown"),
                        "averageRating", Math.round(((Number) r[1]).doubleValue() * 100.0) / 100.0,
                        "reviewCount", r[2]
                ))
                .toList();

        Map<String, Object> report = new HashMap<>();
        report.put("todaySessions", todaySessions);
        report.put("appointmentsPerDay", appointmentsPerDay);
        report.put("physicalVsOnline", physicalVsOnline);
        report.put("noShowRatePercent", noShowRate);
        report.put("totalRevenueLkr", totalRevenue);
        report.put("topDoctors", topDoctors);
        report.put("averageRatings", averageRatings);
        return report;
    }

    private void validateSessionRules(BookingDtos.SessionRequest req, Long excludeSessionId) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session end time must be after start time.");
        }
        // BR-01: Physical requires room; Online must NOT have room
        if ("PHYSICAL".equals(req.getSessionType()) && req.getRoomId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Physical sessions must specify a room.");
        }
        if ("ONLINE".equals(req.getSessionType()) && req.getRoomId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Online sessions must not be assigned a physical room.");
        }
        // BR-02: No overlapping doctor sessions
        if (sessionRepository.existsDoctorOverlap(req.getDoctorId(), req.getSessionDate(), req.getStartTime(), req.getEndTime(), excludeSessionId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This doctor already has an overlapping session.");
        }
        // BR-02: No overlapping room sessions
        if ("PHYSICAL".equals(req.getSessionType())
                && sessionRepository.existsRoomOverlap(req.getRoomId(), req.getSessionDate(), req.getStartTime(), req.getEndTime(), excludeSessionId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Selected room is already booked for another session at this time.");
        }
    }

    private Map<String, Object> toReceptionDto(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("bookingReference", a.getBookingReference());
        m.put("appointmentNumber", a.getAppointmentNumber());
        m.put("patientName", a.getPatient().getUser().getFullName());
        m.put("patientPhone", a.getPatient().getPhone());
        m.put("doctorName", a.getSession().getDoctor().getUser().getFullName());
        m.put("sessionType", a.getSession().getSessionType());
        m.put("estimatedTime", a.getEstimatedTime());
        m.put("status", a.getStatus());
        m.put("paymentStatus", a.getPaymentStatus());
        m.put("paymentMethod", a.getPaymentMethod());
        m.put("feeLkr", a.getFeeLkr());
        // Privacy Rule: NEVER return reasonForVisit, diagnosis, prescription, or chat to Manager
        return m;
    }
}
