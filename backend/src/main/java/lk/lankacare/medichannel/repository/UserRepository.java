package lk.lankacare.medichannel.repository;

import lk.lankacare.medichannel.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailHash(String emailHash);
    boolean existsByEmailHash(String emailHash);
    Page<User> findByRole(String role, Pageable pageable);
    long countByRole(String role);
    long countByActiveTrue();
}
