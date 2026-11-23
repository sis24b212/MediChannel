package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.entity.AuditLog;
import lk.lankacare.medichannel.entity.Doctor;
import lk.lankacare.medichannel.entity.Specialization;
import lk.lankacare.medichannel.entity.User;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.AuditLogRepository;
import lk.lankacare.medichannel.repository.DoctorRepository;
import lk.lankacare.medichannel.repository.SpecializationRepository;
import lk.lankacare.medichannel.repository.UserRepository;
import lk.lankacare.medichannel.security.HmacUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final HmacUtil hmacUtil;
    private final AuditService auditService;

    public AdminService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            DoctorRepository doctorRepository,
            SpecializationRepository specializationRepository,
            PasswordEncoder passwordEncoder,
            HmacUtil hmacUtil,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.doctorRepository = doctorRepository;
        this.specializationRepository = specializationRepository;
        this.passwordEncoder = passwordEncoder;
        this.hmacUtil = hmacUtil;
        this.auditService = auditService;
    }

    @Transactional
    public Map<String, Object> createStaffAccount(Long adminId, String fullName, String email, String role, String tempPassword, String ip) {
        String normalizedRole = role.trim().toUpperCase();
        if (!"DOCTOR".equals(normalizedRole) && !"MANAGER".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin can only create DOCTOR or MANAGER staff accounts.");
        }
        String emailHash = hmacUtil.hashNormalized(email);
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setEmailHash(emailHash);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(normalizedRole);
        user.setActive(true);
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        if ("DOCTOR".equals(normalizedRole)) {
            Specialization defaultSpec = specializationRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No specializations found. Add a specialization in Catalog first."));

            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setSlmcRegNo("SLMC-" + (10000 + user.getId()));
            doctor.setSpecialization(defaultSpec);
            doctor.setQualifications("MBBS");
            doctor.setConsultationFee(new BigDecimal("3000.00"));
            doctor.setBio("Specialist Consultant at Lanka Care Hospital.");
            doctor.setActive(true);
            doctorRepository.save(doctor);
        }

        auditService.record(adminId, "ADMIN", "STAFF_CREATED", "USER", String.valueOf(user.getId()), ip, "Created " + normalizedRole + " account");
        return toAdminUserDto(user);
    }

    public Page> listUsers(String role, int page, int size) {
        Page<User> users = (role != null && !role.isBlank())
                ? userRepository.findByRole(role.trim().toUpperCase(), PageRequest.of(page, size))
                : userRepository.findAll(PageRequest.of(page, size));
        return users.map(this::toAdminUserDto);
    }

    @Transactional
    public Map<String, Object> setUserActive(Long adminId, Long targetUserId, boolean active, String ip) {
        if (adminId.equals(targetUserId) && !active) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrators cannot deactivate their own account (Point 3.4).");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        target.setActive(active);
        userRepository.save(target);

        auditService.record(adminId, "ADMIN", active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", String.valueOf(targetUserId), ip, "Account status updated");
        return toAdminUserDto(target);
    }

    @Transactional
    public Map<String, Object> resetStaffPassword(Long adminId, Long targetUserId, String tempPassword, String ip) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        if ("CUSTOMER".equals(target.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Staff password reset is restricted to DOCTOR and MANAGER accounts.");
        }
        target.setPasswordHash(passwordEncoder.encode(tempPassword));
        target.setMustChangePassword(true);
        target.setFailedLoginAttempts(0);
        target.setLockedUntil(null);
        userRepository.save(target);

        auditService.record(adminId, "ADMIN", "STAFF_PASSWORD_RESET", "USER", String.valueOf(targetUserId), ip, "Temporary password set");
        return Map.of("userId", target.getId(), "mustChangePassword", true);
    }

    public Page<AuditLog> getAuditLogs(String actionFilter, int page, int size) {
        if (actionFilter != null && !actionFilter.isBlank()) {
            return auditLogRepository.findByActionContainingIgnoreCaseOrderByCreatedAtDesc(actionFilter.trim(), PageRequest.of(page, size));
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Map<String, Object> getSystemDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByActiveTrue());
        stats.put("customers", userRepository.countByRole("CUSTOMER"));
        stats.put("doctors", userRepository.countByRole("DOCTOR"));
        stats.put("managers", userRepository.countByRole("MANAGER"));
        stats.put("admins", userRepository.countByRole("ADMIN"));
        stats.put("totalAuditEvents", auditLogRepository.count());
        return stats;
    }

    private Map<String, Object> toAdminUserDto(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("active", u.isActive());
        m.put("mustChangePassword", u.isMustChangePassword());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }
}