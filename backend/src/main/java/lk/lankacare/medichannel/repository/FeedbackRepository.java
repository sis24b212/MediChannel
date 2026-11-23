package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByAppointmentId(Long appointmentId);
    List<Feedback> findByDoctorIdAndModerationStatusOrderByCreatedAtDesc(Long doctorId, String moderationStatus);
    Page<Feedback> findByModerationStatusNotOrderByCreatedAtDesc(String excludedStatus, Pageable pageable);

    @Query("""
        SELECT f.doctor.id, AVG(f.rating), COUNT(f)
        FROM Feedback f
        WHERE f.moderationStatus = 'APPROVED'
        GROUP BY f.doctor.id
    """)
    List<Object[]> averageRatingPerDoctor();
}
