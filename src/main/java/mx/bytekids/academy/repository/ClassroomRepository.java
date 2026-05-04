package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findByIsActiveTrueOrderByGradeLevelAscSectionAsc();
    List<Classroom> findByTeacherAndIsActiveTrue(User teacher);
}
