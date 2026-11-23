package lk.lankacare.medichannel.repository;

import jakarta.persistence.LockModeType;
import lk.lankacare.medichannel.entity.ChannelSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ChannelSessionRepository extends JpaRepository<ChannelSession, Long> {

    /**
     * BR-03: Row-level pessimistic write lock prevents two concurrent bookings
     * from ever receiving the same appointment number or exceeding maxPatients.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChannelSession s WHERE s.id = :id")
    Optional<ChannelSession> findByIdForUpdate(@Param("id") Long id);

    /**
     * BR-02: Checks whether a doctor already has an overlapping non-cancelled session.
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM ChannelSession s
        WHERE s.doctor.id = :doctorId
          AND s.sessionDate = :date
          AND s.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND s.startTime < :endTime
          AND s.endTime > :startTime
    """)
    boolean existsDoctorOverlap(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * BR-02: Checks whether a room already has an overlapping non-cancelled session.
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM ChannelSession s
        WHERE s.room.id = :roomId
          AND s.sessionDate = :date
          AND s.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND s.startTime < :endTime
          AND s.endTime > :startTime
    """)
    boolean existsRoomOverlap(
            @Param("roomId") Long roomId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    List<ChannelSession> findByDoctorIdOrderBySessionDateDescStartTimeAsc(Long doctorId);

    List<ChannelSession> findBySessionDateOrderByStartTimeAsc(LocalDate date);

    Page<ChannelSession> findBySessionDateGreaterThanEqualOrderBySessionDateAscStartTimeAsc(LocalDate fromDate, Pageable pageable);

    @Query("""
        SELECT s FROM ChannelSession s
        WHERE s.status IN ('SCHEDULED', 'OPEN', 'IN_PROGRESS')
          AND (:specializationId IS NULL OR s.doctor.specialization.id = :specializationId)
          AND (:sessionType IS NULL OR s.sessionType = :sessionType)
          AND (:sessionDate IS NULL OR s.sessionDate = :sessionDate)
          AND s.sessionDate >= :today
        ORDER BY s.sessionDate ASC, s.startTime ASC
    """)
    List<ChannelSession> searchActiveSessions(
            @Param("specializationId") Long specializationId,
            @Param("sessionType") String sessionType,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("today") LocalDate today
    );
}
