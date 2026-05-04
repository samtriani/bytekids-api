package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.ClassroomSubject;
import mx.bytekids.academy.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomSubjectRepository extends JpaRepository<ClassroomSubject, UUID> {
    Optional<ClassroomSubject> findByClassroomAndSubject(Classroom classroom, Subject subject);
    List<ClassroomSubject> findByClassroomAndIsActiveTrue(Classroom classroom);
}
