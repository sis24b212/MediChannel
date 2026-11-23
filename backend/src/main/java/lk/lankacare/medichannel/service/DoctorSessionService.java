package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.entity.Appointment;
import lk.lankacare.medichannel.entity.ChannelSession;
import lk.lankacare.medichannel.entity.Consultation;
import lk.lankacare.medichannel.entity.Doctor;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.AppointmentRepository;
import lk.lankacare.medichannel.repository.ChannelSessionRepository;
import lk.lankacare.medichannel.repository.ConsultationRepository;
import lk.lankacare.medichannel.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doctor session workflow (Point 3.2, BR-08, BR-09, RT-01, RT-04).
 */
@Service
public class DoctorSessionService {

    private final DoctorRepository doctorRepository;
    private final ChannelSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final RealtimeEventService realtimeEventService;
    private final PublicCatalogService publicCatalogService;

    public DoctorSessionService(
            DoctorRepository doctorRepository,
            ChannelSessionRepository sessionRepository,
            AppointmentRepository appointmentRepository,
            ConsultationRepository consultationRepository,
            RealtimeEventService realtimeEventService,
            PublicCatalogService publicCatalogService
    ) {
        this.doctorRepository = doctorRepository;
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.realtimeEventService = realtimeEventService;
        this.publicCatalogService = publicCatalogService;
    }

    public List<Map<String, Object>> getMySessions(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor profile not found."));
        return sessionRepository.findByDoctorIdOrderBySessionDateDescStartTimeAsc(doctor.getId())
                .stream()
                .map(publicCatalogService::toSessionDto)
                .toList();
    }

    public List<Map<String, Object>> getSessionPatients(Long doctorUserId, Long sessionId) {
        ChannelSession session = verifyDoctorOwnsSession(doctorUserId, sessionId);
        return appointmentRepository.findBySessionIdOrderByAppointmentNumberAsc(session.getId())
                .stream()
                .map(this::toDoctorPatientRow)
                .toList();
    }

    @Transactional
    public Map<String, Object> startSession(Long doctorUserId, Long sessionId) {
        ChannelSession session = verifyDoctorOwnsSession(doctorUserId, sessionId);
        if ("CANCELLED".equals(session.getStatus()) || "COMPLETED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot start a cancelled or completed session.");
        }
        session.setStatus("IN_PROGRESS");
        sessionRepository.save(session);
        realtimeEventService.broadcastQueueUpdate(session.getId(), session.getCurrentServingNumber(), session.getStatus(), session.getDelayMinutes());
        return publicCatalogService.toSessionDto(session);
    }

    @Transactional
    public Map<String, Object> callNextPatient(Long doctorUserId, Long sessionId) {
        ChannelSession session = verifyDoctorOwnsSession(doctorUserId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            session.setStatus("IN_PROGRESS");
        }

        List<Appointment> queue = appointmentRepository.findBySessionIdOrderByAppointmentNumberAsc(session.getId());

        // Find next eligible patient (BR-08: physical prefers ARRIVED, online calls BOOKED)
        Appointment next = queue.stream()
                .filter(a -> a.getAppointmentNumber() > session.getCurrentServingNumber())
                .filter(a -> "ONLINE".equals(session.getSessionType())
                        ? "BOOKED".equals(a.getStatus())
                        : ("ARRIVED".equals(a.getStatus()) || "BOOKED".equals(a.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No waiting patients remaining in this session."));

        // Close any active IN_CONSULTATION appointment automatically as COMPLETED
        for (Appointment active : queue) {
            if ("IN_CONSULTATION".equals(active.getStatus())) {
                active.setStatus("COMPLETED");
                appointmentRepository.save(active);
                consultationRepository.findByAppointmentId(active.getId()).ifPresent(c -> {
                    c.setRoomOpen(false);
                    c.setEndedAt(Instant.now());
                    consultationRepository.save(c);
                });
            }
        }

        next.setStatus("IN_CONSULTATION");
        appointmentRepository.save(next);

        session.setCurrentServingNumber(next.getAppointmentNumber());
        sessionRepository.save(session);

        // BR-09: Open private online consultation room when doctor starts the appointment
        if ("ONLINE".equals(session.getSessionType())) {
            Consultation consultation = consultationRepository.findByAppointmentId(next.getId()).orElseGet(() -> {
                Consultation c = new Consultation();
                c.setAppointment(next);
                return c;
            });
            consultation.setRoomOpen(true);
            consultation.setStartedAt(Instant.now());
            consultationRepository.save(consultation);

            realtimeEventService.sendUserNotification(
                    next.getPatient().getUser(),
                    "CONSULTATION_STARTED",
                    "Online Consultation Room Open",
                    "Dr. " + session.getDoctor().getUser().getFullName() + " has started your online consultation (No. " + next.getAppointmentNumber() + "). Join now."
            );
        } else {
            realtimeEventService.sendUserNotification(
                    next.getPatient().getUser(),
                    "CALLED",
                    "Your Number Is Being Called — No. " + next.getAppointmentNumber(),
                    "Please proceed to " + (session.getRoom() != null ? session.getRoom().getRoomNumber() : "the consultation room") + "."
            );
        }

        // RT-04: Notify the patient who is 3 numbers away
        int targetNumber = next.getAppointmentNumber() + 3;
        appointmentRepository.findBySessionIdAndAppointmentNumber(session.getId(), targetNumber)
                .filter(a -> !"CANCELLED".equals(a.getStatus()))
                .ifPresent(threeAway -> realtimeEventService.sendUserNotification(
                        threeAway.getPatient().getUser(),
                        "THREE_AWAY",
                        "You Are 3 Numbers Away!",
                        "Now serving No. " + next.getAppointmentNumber() + ". Your appointment is No. " + threeAway.getAppointmentNumber() + "."
                ));

        // RT-01: Broadcast live queue update
        realtimeEventService.broadcastQueueUpdate(session.getId(), session.getCurrentServingNumber(), session.getStatus(), session.getDelayMinutes());

        return toDoctorPatientRow(next);
    }

    @Transactional
    public Map<String, Object> markAppointmentStatus(Long doctorUserId, Long appointmentId, String newStatus) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));
        verifyDoctorOwnsSession(doctorUserId, appt.getSession().getId());

        if (!"COMPLETED".equals(newStatus) && !"NO_SHOW".equals(newStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Doctor can only mark an appointment COMPLETED or NO_SHOW.");
        }

        appt.setStatus(newStatus);
        appointmentRepository.save(appt);

        consultationRepository.findByAppointmentId(appt.getId()).ifPresent(c -> {
            c.setRoomOpen(false);
            c.setEndedAt(Instant.now());
            consultationRepository.save(c);
        });

        ChannelSession s = appt.getSession();
        realtimeEventService.broadcastQueueUpdate(s.getId(), s.getCurrentServingNumber(), s.getStatus(), s.getDelayMinutes());
        return toDoctorPatientRow(appt);
    }

    @Transactional
    public Map<String, Object> endSession(Long doctorUserId, Long sessionId) {
        ChannelSession session = verifyDoctorOwnsSession(doctorUserId, sessionId);
        session.setStatus("COMPLETED");
        sessionRepository.save(session);
        realtimeEventService.broadcastQueueUpdate(session.getId(), session.getCurrentServingNumber(), session.getStatus(), session.getDelayMinutes());
        return publicCatalogService.toSessionDto(session);
    }

    private ChannelSession verifyDoctorOwnsSession(Long doctorUserId, Long sessionId) {
        ChannelSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));
        if (!session.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own channelling sessions.");
        }
        return session;
    }

    private Map<String, Object> toDoctorPatientRow(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("bookingReference", a.getBookingReference());
        m.put("appointmentNumber", a.getAppointmentNumber());
        m.put("patientName", a.getPatient().getUser().getFullName());
        m.put("gender", a.getPatient().getGender());
        m.put("sessionType", a.getSession().getSessionType());
        m.put("estimatedTime", a.getEstimatedTime());
        m.put("status", a.getStatus());
        m.put("paymentStatus", a.getPaymentStatus());
        m.put("reasonForVisit", a.getReasonForVisit());
        return m;
    }
}
