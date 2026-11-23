package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.dto.BookingDtos;
import lk.lankacare.medichannel.entity.*;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.*;
import lk.lankacare.medichannel.security.HmacUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces appointment booking, cancellation, and feedback business rules:
 * BR-03 (atomic unique monotonic numbers with pessimistic lock),
 * BR-04 (one active appointment per patient per session),
 * BR-05 (cutoff 1h physical / 30m online),
 * BR-06 (estimated time formula),
 * BR-07 (unpaid cancellation until 3h before session; never deleted).
 */
@Service
public class AppointmentService {

    private static final ZoneId COLOMBO_ZONE = ZoneId.of("Asia/Colombo");

    private final ChannelSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final FeedbackRepository feedbackRepository;
    private final HmacUtil hmacUtil;
    private final RealtimeEventService realtimeEventService;

    public AppointmentService(
            ChannelSessionRepository sessionRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            FeedbackRepository feedbackRepository,
            HmacUtil hmacUtil,
            RealtimeEventService realtimeEventService
    ) {
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.feedbackRepository = feedbackRepository;
        this.hmacUtil = hmacUtil;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public Map<String, Object> bookForPatient(Long userId, BookingDtos.CreateAppointmentRequest req) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient profile not found."));
        return createAppointmentInternal(req.getSessionId(), patient, req.getReasonForVisit(), false);
    }

    @Transactional
    public Map<String, Object> bookWalkIn(BookingDtos.WalkInBookingRequest req) {
        String nicHash = hmacUtil.hashNormalized(req.getPatientNic());
        Patient patient = patientRepository.findByNicHash(nicHash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No registered patient found for this NIC."));
        return createAppointmentInternal(req.getSessionId(), patient, req.getReasonForVisit(), true);
    }

    private Map<String, Object> createAppointmentInternal(Long sessionId, Patient patient, String reason, boolean walkIn) {
        // BR-03: Pessimistic write lock on session guarantees strictly unique, monotonic appointment numbers
        ChannelSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        if ("CANCELLED".equals(session.getStatus()) || "COMPLETED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot book a cancelled or completed session.");
        }

        // BR-05: Booking cutoff check (skip for reception walk-in when session is OPEN/IN_PROGRESS)
        if (!walkIn) {
            ZonedDateTime sessionStart = ZonedDateTime.of(session.getSessionDate(), session.getStartTime(), COLOMBO_ZONE);
            ZonedDateTime now = ZonedDateTime.now(COLOMBO_ZONE);
            long minutesUntilStart = Duration.between(now, sessionStart).toMinutes();
            int cutoff = "PHYSICAL".equals(session.getSessionType()) ? 60 : 30;
            if (minutesUntilStart < cutoff) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Booking closes " + cutoff + " minutes before a " + session.getSessionType().toLowerCase() + " session (BR-05)."
                );
            }
        }

        // BR-04: One active appointment per patient per session
        if (appointmentRepository.existsActiveAppointmentInSession(session.getId(), patient.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active appointment in this session (BR-04).");
        }

        // BR-03: Never exceed maxPatients; never reuse cancelled numbers
        if (session.getLastIssuedNumber() >= session.getMaxPatients()) {
            throw new ApiException(HttpStatus.CONFLICT, "This session is fully booked (BR-03).");
        }

        int nextNumber = session.getLastIssuedNumber() + 1;
        session.setLastIssuedNumber(nextNumber);
        sessionRepository.save(session);

        // BR-06: Estimated time = start time + (number - 1) * minutesPerPatient + session delay
        LocalTime estimatedTime = session.getStartTime()
                .plusMinutes((long) (nextNumber - 1) * session.getMinutesPerPatient() + session.getDelayMinutes());

        String bookingRef = "MC-" + LocalDate.now(COLOMBO_ZONE).getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Appointment appt = new Appointment();
        appt.setBookingReference(bookingRef);
        appt.setSession(session);
        appt.setPatient(patient);
        appt.setAppointmentNumber(nextNumber);
        appt.setEstimatedTime(estimatedTime);
        appt.setReasonForVisit(reason.trim());
        appt.setStatus("BOOKED");
        appt.setPaymentStatus("PENDING");
        appt.setFeeLkr(session.getFeeLkr());
        appt.setBookedByWalkin(walkIn);
        appt = appointmentRepository.save(appt);

        // RT-02: Broadcast live numbers left immediately
        realtimeEventService.broadcastAvailabilityUpdate(
                session.getId(),
                session.getRemainingNumbers(),
                session.getLastIssuedNumber(),
                session.getMaxPatients()
        );

        // RT-04: Confirmation notification
        realtimeEventService.sendUserNotification(
                patient.getUser(),
                "BOOKING_CONFIRMED",
                "Appointment Confirmed — No. " + nextNumber,
                "Booked with " + session.getDoctor().getUser().getFullName()
                        + " on " + session.getSessionDate() + " (Est. " + estimatedTime + "). Ref: " + bookingRef
        );

        return toPatientAppointmentDto(appt, !walkIn);
    }

    public List<Map<String, Object>> getMyAppointments(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient profile not found."));
        return appointmentRepository.findByPatientIdOrderBySessionSessionDateDescAppointmentNumberAsc(patient.getId())
                .stream()
                .map(a -> toPatientAppointmentDto(a, true))
                .toList();
    }

    @Transactional
    public Map<String, Object> cancelMyAppointment(Long userId, Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));

        if (!appt.getPatient().getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own appointments.");
        }
        if (!"BOOKED".equals(appt.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only booked appointments can be cancelled.");
        }
        if ("PAID".equals(appt.getPaymentStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paid appointments cannot be cancelled online (BR-07).");
        }

        // BR-07: Cancel unpaid appointment until 3 hours before session; kept in DB (not deleted)
        ZonedDateTime sessionStart = ZonedDateTime.of(
                appt.getSession().getSessionDate(),
                appt.getSession().getStartTime(),
                COLOMBO_ZONE
        );
        if (Duration.between(ZonedDateTime.now(COLOMBO_ZONE), sessionStart).toMinutes() < 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cancellation closes 3 hours before the session start time (BR-07).");
        }

        appt.setStatus("CANCELLED");
        appt.setCancelledAt(Instant.now());
        appointmentRepository.save(appt);

        ChannelSession s = appt.getSession();
        // Note: BR-03 appointment numbers are NEVER reused, so remainingNumbers remains maxPatients - lastIssuedNumber
        realtimeEventService.broadcastAvailabilityUpdate(
                s.getId(),
                s.getRemainingNumbers(),
                s.getLastIssuedNumber(),
                s.getMaxPatients()
        );

        return toPatientAppointmentDto(appt, true);
    }

    @Transactional
    public Map<String, Object> submitFeedback(Long userId, BookingDtos.FeedbackRequest req) {
        Appointment appt = appointmentRepository.findById(req.getAppointmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));

        if (!appt.getPatient().getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only review your own completed appointment.");
        }
        if (!"COMPLETED".equals(appt.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feedback can only be submitted after a completed consultation.");
        }
        if (feedbackRepository.existsByAppointmentId(appt.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already submitted feedback for this appointment.");
        }

        Feedback fb = new Feedback();
        fb.setAppointment(appt);
        fb.setDoctor(appt.getSession().getDoctor());
        fb.setPatient(appt.getPatient());
        fb.setRating(req.getRating());
        fb.setComment(req.getComment() != null ? req.getComment().trim() : "");
        fb.setModerationStatus("PENDING");
        fb = feedbackRepository.save(fb);

        Map<String, Object> out = new HashMap<>();
        out.put("id", fb.getId());
        out.put("rating", fb.getRating());
        out.put("comment", fb.getComment());
        out.put("moderationStatus", fb.getModerationStatus());
        return out;
    }

    public Map<String, Object> toPatientAppointmentDto(Appointment a, boolean includeReasonForVisit) {
        ChannelSession s = a.getSession();
        int ahead = 0;
        if ("IN_PROGRESS".equals(s.getStatus()) && a.getAppointmentNumber() > s.getCurrentServingNumber()
                && ("BOOKED".equals(a.getStatus()) || "ARRIVED".equals(a.getStatus()))) {
            ahead = Math.max(0, a.getAppointmentNumber() - s.getCurrentServingNumber() - 1);
        }

        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("bookingReference", a.getBookingReference());
        m.put("sessionId", s.getId());
        m.put("doctorName", s.getDoctor().getUser().getFullName());
        m.put("specializationName", s.getDoctor().getSpecialization().getName());
        m.put("sessionDate", s.getSessionDate());
        m.put("startTime", s.getStartTime());
        m.put("sessionType", s.getSessionType());
        m.put("sessionStatus", s.getStatus());
        m.put("roomNumber", s.getRoom() != null ? s.getRoom().getRoomNumber() : null);
        m.put("appointmentNumber", a.getAppointmentNumber());
        m.put("estimatedTime", a.getEstimatedTime());
        m.put("delayMinutes", s.getDelayMinutes());
        m.put("currentServingNumber", s.getCurrentServingNumber());
        m.put("patientsAhead", ahead);
        m.put("status", a.getStatus());
        m.put("paymentStatus", a.getPaymentStatus());
        m.put("paymentMethod", a.getPaymentMethod());
        m.put("feeLkr", a.getFeeLkr());
        // Privacy Rule: only include reasonForVisit for patient/doctor, never for manager/admin
        if (includeReasonForVisit) {
            m.put("reasonForVisit", a.getReasonForVisit());
        }
        return m;
    }
}
