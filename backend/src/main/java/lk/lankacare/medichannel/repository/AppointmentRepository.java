package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByBookingReference(String bookingReference);

    List<Appointment> findBySessionIdOrderByAppointmentNumberAsc(Long sessionId);

    List<Appointment> findByPatientIdOrderBySessionSessionDateDescAppointmentNumberAsc(Long patientId);

    Page<Appointment> findBySessionSessionDateOrderBySessionStartTimeAscAppointmentNumberAsc(LocalDate date, Pageable pageable);

    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.session.id = :sessionId
          AND a.patient.id = :patientId
          AND a.status <> 'CANCELLED'
    """)
    boolean existsActiveAppointmentInSession(@Param("sessionId") Long sessionId, @Param("patientId") Long patientId);

    Optional<Appointment> findBySessionIdAndAppointmentNumber(Long sessionId, int appointmentNumber);

    // ---- Manager Reports (SQL / JPQL Aggregations - Point 3.3) ----

    @Query("SELECT a.session.sessionDate, COUNT(a) FROM Appointment a WHERE a.status <> 'CANCELLED' GROUP BY a.session.sessionDate ORDER BY a.session.sessionDate DESC")
    List<Object[]> countAppointmentsPerDay();

    @Query("SELECT a.session.sessionType, COUNT(a) FROM Appointment a WHERE a.status <> 'CANCELLED' GROUP BY a.session.sessionType")
    List<Object[]> countPhysicalVsOnline();

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = 'NO_SHOW'")
    long countNoShows();

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status IN ('COMPLETED', 'NO_SHOW')")
    long countFinishedAppointments();

    @Query("SELECT COALESCE(SUM(a.feeLkr), 0) FROM Appointment a WHERE a.paymentStatus = 'PAID'")
    BigDecimal sumPaidRevenue();

    @Query("""
        SELECT a.session.doctor.id, COUNT(a)
        FROM Appointment a
        WHERE a.status <> 'CANCELLED'
        GROUP BY a.session.doctor.id
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> findTopDoctorsByAppointmentCount();
}
