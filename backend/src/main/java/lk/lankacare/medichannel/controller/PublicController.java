package lk.lankacare.medichannel.controller;

import lk.lankacare.medichannel.service.PublicCatalogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicCatalogService publicCatalogService;

    public PublicController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<Map<String, Object>>> getSpecializations() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(publicCatalogService.getActiveSpecializations());
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> getDoctors() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(publicCatalogService.getActiveDoctors());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> searchSessions(
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String sessionType
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(publicCatalogService.searchSessions(doctorName, specializationId, date, sessionType));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(publicCatalogService.getSessionById(id));
    }

    @GetMapping("/notices")
    public ResponseEntity<List<Map<String, Object>>> getNotices() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic())
                .body(publicCatalogService.getActiveNotices());
    }
}
