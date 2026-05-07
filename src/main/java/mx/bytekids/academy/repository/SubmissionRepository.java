package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByStudentOrderBySubmittedAtDesc(User student);
    List<Submission> findByContentOrderBySubmittedAtDesc(Content content);
    List<Submission> findByStatusOrderBySubmittedAtDesc(SubmissionStatus status);
    Optional<Submission> findTopByStudentAndContentOrderBySubmittedAtDesc(User student, Content content);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.status = 'aprobado'")
    long countApprovedByStudent(User student);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.status = :status AND s.content.subject.name = :subjectName")
    long countApprovedByStudentAndSubject(@Param("student") User student,
                                          @Param("status") SubmissionStatus status,
                                          @Param("subjectName") String subjectName);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.student = :student AND s.status = :status AND s.content.type = :contentType")
    long countApprovedByStudentAndType(@Param("student") User student,
                                       @Param("status") SubmissionStatus status,
                                       @Param("contentType") ContentType contentType);
}
