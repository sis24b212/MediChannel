package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByActiveTrueOrderByIdAsc();
    List<Doctor> findBySpecializationIdAndActiveTrue(Long specializationId);
    boolean existsBySlmcRegNoIgnoreCase(String slmcRegNo);
}
