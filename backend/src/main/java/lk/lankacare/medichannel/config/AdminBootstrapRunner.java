package lk.lankacare.medichannel.config;

import lk.lankacare.medichannel.entity.*;
import lk.lankacare.medichannel.repository.*;
import lk.lankacare.medichannel.security.HmacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Startup bootstrap:
 * 1. Always ensures the initial ADMIN account exists from environment variables (Point 2).
 * 2. When SEED_DEMO_DATA=true and the database has no doctors yet, seeds realistic fictional
 *    Sri Lankan hospital data encrypted via AesGcmConverter & HmacUtil (Point 7).
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final SpecializationRepository specializationRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;
    private final ChannelSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final FeedbackRepository feedbackRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;
    private final HmacUtil hmacUtil;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PatientRepository patientRepository,
            SpecializationRepository specializationRepository,
            RoomRepository roomRepository,
            DoctorRepository doctorRepository,
            ChannelSessionRepository sessionRepository,
            AppointmentRepository appointmentRepository,
            ConsultationRepository consultationRepository,
            FeedbackRepository feedbackRepository,
            NoticeRepository noticeRepository,
            PasswordEncoder passwordEncoder,
            HmacUtil hmacUtil
    ) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.specializationRepository = specializationRepository;
        this.roomRepository = roomRepository;
        this.doctorRepository = doctorRepository;
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.feedbackRepository = feedbackRepository;
        this.noticeRepository = noticeRepository;
        this.passwordEncoder = passwordEncoder;
        this.hmacUtil = hmacUtil;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureInitialAdmin();
        if (seedEnabled && doctorRepository.count() == 0) {
            seedFictionalHospitalData(admin);
            log.info("MediChannel fictional seed data initialized with AES-256-GCM encryption.");
        }
    }

    private User ensureInitialAdmin() {
        String emailHash = hmacUtil.hashNormalized(adminEmail);
        return userRepository.findByEmailHash(emailHash).orElseGet(() -> {
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD environment variable must be set.");
            }
            User u = new User();
            u.setEmail(adminEmail.trim().toLowerCase());
            u.setEmailHash(emailHash);
            u.setPasswordHash(passwordEncoder.encode(adminPassword));
            u.setFullName(adminName);
            u.setRole("ADMIN");
            u.setActive(true);
            u.setMustChangePassword(false);
            log.info("Created initial ADMIN account from environment variables.");
            return userRepository.save(u);
        });
    }

    private void seedFictionalHospitalData(User admin) {
        // 1. Specializations
        Specialization cardiology = findOrCreateSpec("Cardiology", "Heart, ECG, and cardiovascular care.");
        Specialization pediatrics = findOrCreateSpec("Pediatrics", "Child and adolescent healthcare.");
        Specialization dermatology = findOrCreateSpec("Dermatology", "Skin, hair, and allergy clinics.");
        Specialization generalMed = findOrCreateSpec("General Medicine", "Internal medicine and chronic care.");

        // 2. Rooms
        Room r201 = findOrCreateRoom("Room 201", "2nd Floor", "Central Wing");
        Room r204 = findOrCreateRoom("Room 204", "2nd Floor", "Child Care Wing");
        Room r101 = findOrCreateRoom("Room 101", "1st Floor", "East Wing");

        // 3. Manager account
        User manager = createUserIfAbsent("manager@lankacare.lk", "Manager@123", "Nimali Perera", "MANAGER");

        // 4. Doctors
        User docUser1 = createUserIfAbsent("dr.silva@lankacare.lk", "Doctor@123", "Dr. Kamal Silva", "DOCTOR");
        User docUser2 = createUserIfAbsent("dr.fernando@lankacare.lk", "Doctor@123", "Dr. Dilani Fernando", "DOCTOR");
        User docUser3 = createUserIfAbsent("dr.jayawardena@lankacare.lk", "Doctor@123", "Dr. Ruwan Jayawardena", "DOCTOR");

        Doctor drSilva = createDoctor(docUser1, "SLMC-24810", cardiology, "MBBS (Colombo), MD, MRCP (UK)", new BigDecimal("3500.00"), "Senior Consultant Cardiologist with 16 years clinical experience.");
        Doctor drFernando = createDoctor(docUser2, "SLMC-27145", pediatrics, "MBBS, DCH, MD (Paediatrics)", new BigDecimal("2800.00"), "Consultant Paediatrician focusing on early childhood nutrition and asthma care.");
        Doctor drJaya = createDoctor(docUser3, "SLMC-30219", dermatology, "MBBS, MD (Dermatology)", new BigDecimal("3000.00"), "Consultant Dermatologist offering physical clinics and online tele-dermatology.");

        // 5. Patients (Fictional Sri Lankan residents - all encrypted via AesGcmConverter)
        Patient pat1 = createPatient("kasun@example.lk", "Patient@123", "Kasun Wickramasinghe", "199512345678", "1995-04-14", "MALE", "0771234567", "42 Galle Road, Dehiwala, Colombo");
        Patient pat2 = createPatient("sanduni@example.lk", "Patient@123", "Sanduni Ratnayake", "985621478V", "1998-09-22", "FEMALE", "0712345678", "15 Havelock Road, Colombo 05");

        // 6. Sessions (Today & Tomorrow, both PHYSICAL and ONLINE)
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        ChannelSession s1 = createSession(drSilva, r201, today, LocalTime.of(16, 0), LocalTime.of(19, 0), "PHYSICAL", 20, 15, new BigDecimal("3500.00"), "IN_PROGRESS", 1, 3);
        ChannelSession s2 = createSession(drJaya, null, today, LocalTime.of(18, 0), LocalTime.of(20, 0), "ONLINE", 15, 10, new BigDecimal("3000.00"), "OPEN", 0, 1);
        ChannelSession s3 = createSession(drFernando, r204, tomorrow, LocalTime.of(9, 0), LocalTime.of(12, 0), "PHYSICAL", 25, 10, new BigDecimal("2800.00"), "OPEN", 0, 0);
        ChannelSession s4 = createSession(drSilva, r101, today.minusDays(2), LocalTime.of(16, 0), LocalTime.of(18, 0), "PHYSICAL", 15, 15, new BigDecimal("3500.00"), "COMPLETED", 2, 2);

        // 7. Appointments covering every status (COMPLETED, NO_SHOW, IN_CONSULTATION, ARRIVED, BOOKED, CANCELLED)
        Appointment aPastCompleted = createAppointment("MC-2026-0001", s4, pat1, 1, LocalTime.of(16, 0), "Routine blood pressure and ECG review", "COMPLETED", "PAID", "CARD");
        createAppointment("MC-2026-0002", s4, pat2, 2, LocalTime.of(16, 15), "Chest tightness follow-up", "NO_SHOW", "PENDING", null);

        Appointment aActive1 = createAppointment("MC-2026-0101", s1, pat1, 1, LocalTime.of(16, 0), "Follow-up lipid profile check", "IN_CONSULTATION", "PAID", "CASH");
        createAppointment("MC-2026-0102", s1, pat2, 2, LocalTime.of(16, 15), "Mild palpitations during exercise", "ARRIVED", "PAID", "CARD");
        createAppointment("MC-2026-0103", s2, pat1, 1, LocalTime.of(18, 0), "Recurring skin rash on forearm", "BOOKED", "PENDING", null);

        // Consultation record for completed past appointment
        Consultation c1 = new Consultation();
        c1.setAppointment(aPastCompleted);
        c1.setDiagnosis("Essential hypertension — well controlled (124/78 mmHg)");
        c1.setAdvice("Continue low-sodium diet, 30 min daily brisk walking, review in 3 months.");
        c1.setPrescription("1. Losartan 50mg — 1 tablet daily after breakfast (90 days)\n2. Atorvastatin 10mg — 1 tablet at night (90 days)");
        c1.setRoomOpen(false);
        c1.setStartedAt(Instant.now().minusSeconds(172800));
        c1.setEndedAt(Instant.now().minusSeconds(171900));
        consultationRepository.save(c1);

        // Approved feedback for Dr. Silva
        Feedback fb = new Feedback();
        fb.setAppointment(aPastCompleted);
        fb.setDoctor(drSilva);
        fb.setPatient(pat1);
        fb.setRating(5);
        fb.setComment("Very attentive consultation and accurate live queue timing.");
        fb.setModerationStatus("APPROVED");
        feedbackRepository.save(fb);

        // Public Hospital Notices
        Notice n1 = new Notice();
        n1.setTitle("Online Tele-Consultations Now Open Daily 6 PM – 9 PM");
        n1.setContent("Patients can now book encrypted online consultations directly with Lanka Care Hospital specialists and download printable prescriptions immediately after the visit.");
        n1.setPriority("IMPORTANT");
        n1.setCreatedBy(manager);
        noticeRepository.save(n1);

        Notice n2 = new Notice();
        n2.setTitle("OPD Parking & Reception Check-In Reminder");
        n2.setContent("Please arrive 15 minutes before your estimated appointment time and check in at the Ground Floor Channelling Reception with your booking reference.");
        n2.setPriority("NORMAL");
        n2.setCreatedBy(manager);
        noticeRepository.save(n2);
    }

    private Specialization findOrCreateSpec(String name, String desc) {
        return specializationRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Specialization s = new Specialization();
                    s.setName(name);
                    s.setDescription(desc);
                    return specializationRepository.save(s);
                });
    }

    private Room findOrCreateRoom(String num, String floor, String wing) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(num))
                .findFirst()
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber(num);
                    r.setFloor(floor);
                    r.setWing(wing);
                    return roomRepository.save(r);
                });
    }

    private User createUserIfAbsent(String email, String rawPassword, String fullName, String role) {
        String hash = hmacUtil.hashNormalized(email);
        return userRepository.findByEmailHash(hash).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setEmailHash(hash);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setFullName(fullName);
            u.setRole(role);
            return userRepository.save(u);
        });
    }

    private Doctor createDoctor(User u, String slmc, Specialization spec, String qual, BigDecimal fee, String bio) {
        Doctor d = new Doctor();
        d.setUser(u);
        d.setSlmcRegNo(slmc);
        d.setSpecialization(spec);
        d.setQualifications(qual);
        d.setConsultationFee(fee);
        d.setBio(bio);
        return doctorRepository.save(d);
    }

    private Patient createPatient(String email, String pw, String name, String nic, String dob, String gender, String phone, String address) {
        User u = createUserIfAbsent(email, pw, name, "CUSTOMER");
        Patient p = new Patient();
        p.setUser(u);
        p.setNic(nic);
        p.setNicHash(hmacUtil.hashNormalized(nic));
        p.setDateOfBirth(dob);
        p.setGender(gender);
        p.setPhone(phone);
        p.setAddress(address);
        return patientRepository.save(p);
    }

    private ChannelSession createSession(Doctor d, Room r, LocalDate date, LocalTime start, LocalTime end, String type, int max, int minPer, BigDecimal fee, String status, int serving, int issued) {
        ChannelSession s = new ChannelSession();
        s.setDoctor(d);
        s.setRoom(r);
        s.setSessionDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSessionType(type);
        s.setMaxPatients(max);
        s.setMinutesPerPatient(minPer);
        s.setFeeLkr(fee);
        s.setStatus(status);
        s.setCurrentServingNumber(serving);
        s.setLastIssuedNumber(issued);
        return sessionRepository.save(s);
    }

    private Appointment createAppointment(String ref, ChannelSession s, Patient p, int num, LocalTime est, String reason, String status, String payStatus, String payMethod) {
        Appointment a = new Appointment();
        a.setBookingReference(ref);
        a.setSession(s);
        a.setPatient(p);
        a.setAppointmentNumber(num);
        a.setEstimatedTime(est);
        a.setReasonForVisit(reason);
        a.setStatus(status);
        a.setPaymentStatus(payStatus);
        a.setPaymentMethod(payMethod);
        a.setFeeLkr(s.getFeeLkr());
        return appointmentRepository.save(a);
    }
}
