package lk.lankacare.medichannel.controller;

import jakarta.servlet.http.HttpServletRequest;
import lk.lankacare.medichannel.entity.AuditLog;
import lk.lankacare.medichannel.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(adminService.getSystemDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(adminService.listUsers(role, page, size));
    }

    @PostMapping("/staff")
    public ResponseEntity<Map<String, Object>> createStaff(
            Authentication auth,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpReq
    ) {
        Long adminId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createStaffAccount(
                        adminId,
                        body.getOrDefault("fullName", ""),
                        body.getOrDefault("email", ""),
                        body.getOrDefault("role", ""),
                        body.getOrDefault("temporaryPassword", ""),
                        httpReq.getRemoteAddr()
                ));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleActive(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest httpReq
    ) {
        Long adminId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                adminService.setUserActive(adminId, id, Boolean.TRUE.equals(body.get("active")), httpReq.getRemoteAddr())
        );
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetStaffPassword(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpReq
    ) {
        Long adminId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                adminService.resetStaffPassword(adminId, id, body.getOrDefault("temporaryPassword", ""), httpReq.getRemoteAddr())
        );
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> auditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(adminService.getAuditLogs(action, page, size));
    }
}
