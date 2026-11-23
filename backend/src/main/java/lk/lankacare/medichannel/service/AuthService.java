package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.config.JwtUtil;
import lk.lankacare.medichannel.dto.AuthDtos;
import lk.lankacare.medichannel.entity.Patient;
import lk.lankacare.medichannel.entity.User;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.PatientRepository;
import lk.lankacare.medichannel.repository.UserRepository;
import lk.lankacare.medichannel.security.HmacUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles public CUSTOMER registration, login with 5-attempt 15-minute lockout,
 * single generic login error, and forced password change (Point 2, 3.1, BR-10).
 */
@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final HmacUtil hmacUtil;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            HmacUtil hmacUtil,
            JwtUtil jwtUtil,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.hmacUtil = hmacUtil;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    @Transactional
    public AuthDtos.AuthResponse registerPatient(AuthDtos.RegisterRequest req, String ip) {
        String emailHash = hmacUtil.hashNormalized(req.getEmail());
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        String nicHash = hmacUtil.hashNormalized(req.getNic());
        if (patientRepository.existsByNicHash(nicHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "A patient with this NIC is already registered.");
        }

        // Public registration ALWAYS creates role CUSTOMER (Point 2)
        User user = new User();
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setEmailHash(emailHash);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName().trim());
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setMustChangePassword(false);
        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setNic(req.getNic().trim().toUpperCase());
        patient.setNicHash(nicHash);
        patient.setDateOfBirth(req.getDateOfBirth().trim());
        patient.setGender(req.getGender().trim().toUpperCase());
        patient.setPhone(req.getPhone().trim());
        patient.setAddress(req.getAddress().trim());
        patientRepository.save(patient);

        auditService.record(user.getId(), "CUSTOMER", "PATIENT_REGISTERED", "USER", String.valueOf(user.getId()), ip, "New patient account registered");

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), false);
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole(), false);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req, String ip) {
        String emailHash = hmacUtil.hashNormalized(req.getEmail());
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> {
                    auditService.record(null, "GUEST", "LOGIN_FAILED", "AUTH", "UNKNOWN", ip, "Invalid login attempt");
                    return new ApiException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR);
                });

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been deactivated. Contact IT Administration.");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account temporarily locked due to multiple failed login attempts. Try again in 15 minutes.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);
            auditService.record(user.getId(), user.getRole(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()), ip, "Failed password attempt");
            throw new ApiException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR);
        }

        // Successful login resets lockout counters
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditService.record(user.getId(), user.getRole(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), ip, "User logged in");

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.isMustChangePassword());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isMustChangePassword());
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest req, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditService.record(user.getId(), user.getRole(), "PASSWORD_CHANGED", "USER", String.valueOf(user.getId()), ip, "User updated password");
    }
}
