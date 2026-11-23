package lk.lankacare.medichannel.service;

import lk.lankacare.medichannel.config.CacheConfig;
import lk.lankacare.medichannel.entity.ChannelSession;
import lk.lankacare.medichannel.entity.Doctor;
import lk.lankacare.medichannel.entity.Notice;
import lk.lankacare.medichannel.entity.Specialization;
import lk.lankacare.medichannel.exception.ApiException;
import lk.lankacare.medichannel.repository.ChannelSessionRepository;
import lk.lankacare.medichannel.repository.DoctorRepository;
import lk.lankacare.medichannel.repository.NoticeRepository;
import lk.lankacare.medichannel.repository.SpecializationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves public catalog data with TTL caching (Point 7 Performance).
 */
@Service
public class PublicCatalogService {

    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;
    private final ChannelSessionRepository sessionRepository;
    private final NoticeRepository noticeRepository;

    public PublicCatalogService(
            SpecializationRepository specializationRepository,
            DoctorRepository doctorRepository,
            ChannelSessionRepository sessionRepository,
            NoticeRepository noticeRepository
    ) {
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
        this.sessionRepository = sessionRepository;
        this.noticeRepository = noticeRepository;
    }

    @Cacheable(CacheConfig.SPECIALIZATIONS_CACHE)
    public List<Map<String, Object>> getActiveSpecializations() {
        return specializationRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toSpecDto)
                .toList();
    }

    @Cacheable(CacheConfig.DOCTORS_CACHE)
    public List<Map<String, Object>> getActiveDoctors() {
        return doctorRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toDoctorDto)
                .toList();
    }

    public List<Map<String, Object>> searchSessions(String doctorName, Long specializationId, LocalDate date, String sessionType) {
        String normalizedType = (sessionType != null && !sessionType.isBlank()) ? sessionType.trim().toUpperCase() : null;
        List<ChannelSession> found = sessionRepository.searchActiveSessions(
                specializationId,
                normalizedType,
                date,
                LocalDate.now()
        );
        String nameFilter = doctorName != null ? doctorName.trim().toLowerCase() : "";
        return found.stream()
                .filter(s -> nameFilter.isEmpty() || s.getDoctor().getUser().getFullName().toLowerCase().contains(nameFilter))
                .map(this::toSessionDto)
                .toList();
    }

    public Map<String, Object> getSessionById(Long sessionId) {
        ChannelSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channelling session not found."));
        return toSessionDto(session);
    }

    public List<Map<String, Object>> getActiveNotices() {
        return noticeRepository.findByActiveTrueOrderByPublishedAtDesc().stream()
                .map(n -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("title", n.getTitle());
                    m.put("content", n.getContent());
                    m.put("priority", n.getPriority());
                    m.put("publishedAt", n.getPublishedAt());
                    return m;
                })
                .toList();
    }

    private Map<String, Object> toSpecDto(Specialization s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        return m;
    }

    private Map<String, Object> toDoctorDto(Doctor d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("fullName", d.getUser().getFullName());
        m.put("slmcRegNo", d.getSlmcRegNo());
        m.put("specializationId", d.getSpecialization().getId());
        m.put("specializationName", d.getSpecialization().getName());
        m.put("qualifications", d.getQualifications());
        m.put("consultationFee", d.getConsultationFee());
        m.put("bio", d.getBio());
        return m;
    }

    public Map<String, Object> toSessionDto(ChannelSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("doctorId", s.getDoctor().getId());
        m.put("doctorName", s.getDoctor().getUser().getFullName());
        m.put("slmcRegNo", s.getDoctor().getSlmcRegNo());
        m.put("specializationName", s.getDoctor().getSpecialization().getName());
        m.put("roomNumber", s.getRoom() != null ? s.getRoom().getRoomNumber() : null);
        m.put("roomFloor", s.getRoom() != null ? s.getRoom().getFloor() : null);
        m.put("sessionDate", s.getSessionDate());
        m.put("startTime", s.getStartTime());
        m.put("endTime", s.getEndTime());
        m.put("sessionType", s.getSessionType());
        m.put("maxPatients", s.getMaxPatients());
        m.put("minutesPerPatient", s.getMinutesPerPatient());
        m.put("feeLkr", s.getFeeLkr());
        m.put("status", s.getStatus());
        m.put("delayMinutes", s.getDelayMinutes());
        m.put("currentServingNumber", s.getCurrentServingNumber());
        m.put("lastIssuedNumber", s.getLastIssuedNumber());
        m.put("remainingNumbers", s.getRemainingNumbers());
        return m;
    }
}
