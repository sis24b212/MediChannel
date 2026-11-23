package lk.lankacare.medichannel.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import lk.lankacare.medichannel.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByActiveTrueOrderByIdAsc();
    List<Doctor> findBySpecializationIdAndActiveTrue(Long specializationId);
    boolean existsBySlmcRegNoIgnoreCase(String slmcRegNo);
}
