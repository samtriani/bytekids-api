package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    boolean existsByUsername(String username);
    List<User> findByRoleAndIsActiveTrue(UserRole role);
}
