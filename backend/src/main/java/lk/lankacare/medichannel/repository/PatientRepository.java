package lk.lankacare.medichannel.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import lk.lankacare.medichannel.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<Patient> findByUserId(Long userId);
    Optional<Patient> findByNicHash(String nicHash);
    boolean existsByNicHash(String nicHash);
}
