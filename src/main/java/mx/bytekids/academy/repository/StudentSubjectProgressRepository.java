package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.StudentSubjectProgress;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentSubjectProgressRepository extends JpaRepository<StudentSubjectProgress, UUID> {
    List<StudentSubjectProgress> findByStudentOrderByXpInSubjectDesc(User student);
    Optional<StudentSubjectProgress> findByStudentAndSubject(User student, Subject subject);
}
