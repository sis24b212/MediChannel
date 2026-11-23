package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.entity.AuditLog;
import lk.lankacare.medichannel.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/**
 * Append-only audit logging service (Point 3.4).
 * Never stores personal details, tokens, or medical data.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long userId, String role, String action, String resourceType, String resourceId, String ipAddress, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUserRole(role);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setIpAddress(ipAddress != null ? ipAddress : "unknown");
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
