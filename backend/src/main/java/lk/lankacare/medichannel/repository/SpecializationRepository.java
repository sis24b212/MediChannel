package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    List<Specialization> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
