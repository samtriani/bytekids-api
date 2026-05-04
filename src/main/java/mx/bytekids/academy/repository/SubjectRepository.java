package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByIsActiveTrueOrderByName();
    boolean existsByName(String name);
}
