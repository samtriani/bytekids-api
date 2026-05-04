package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.QuizAttempt;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByStudentOrderByCompletedAtDesc(User student);
    List<QuizAttempt> findByContentOrderByCompletedAtDesc(Content content);
}
