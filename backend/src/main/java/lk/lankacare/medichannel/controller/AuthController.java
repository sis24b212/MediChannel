package lk.lankacare.medichannel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.lankacare.medichannel.dto.AuthDtos;
import lk.lankacare.medichannel.service.AuthService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest req,
            HttpServletRequest httpReq
    ) {
        AuthDtos.AuthResponse res = authService.registerPatient(req, httpReq.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest req,
            HttpServletRequest httpReq
    ) {
        AuthDtos.AuthResponse res = authService.login(req, httpReq.getRemoteAddr());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(res);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest req,
            HttpServletRequest httpReq
    ) {
        Long userId = (Long) auth.getPrincipal();
        authService.changePassword(userId, req, httpReq.getRemoteAddr());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Password updated successfully."));
    }
}
