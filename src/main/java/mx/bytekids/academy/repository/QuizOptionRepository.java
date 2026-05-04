package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.QuizOption;
import mx.bytekids.academy.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {
    List<QuizOption> findByQuestionOrderByOrderIndexAsc(QuizQuestion question);
}
