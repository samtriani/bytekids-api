package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.QuizAttempt;
import mx.bytekids.academy.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, UUID> {
    List<QuizAttemptAnswer> findByAttempt(QuizAttempt attempt);
}
