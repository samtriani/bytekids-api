package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.quiz.QuizQuestionResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ContentService contentService;
    private final UserService userService;
    private final ProgressService progressService;

    /**
     * Preguntas con sus opciones. Antes devolvia la entidad cruda, que no tiene
     * relacion a opciones: el alumno veia la pregunta y ningun lugar donde
     * responder. Se arma el DTO para incluirlas sin filtrar cual es la correcta.
     */
    public List<QuizQuestionResponse> findQuestions(UUID contentId) {
        Content content = contentService.findById(contentId);
        return questionRepository.findByContentOrderByOrderIndexAsc(content).stream()
                .map(q -> QuizQuestionResponse.from(
                        q, optionRepository.findByQuestionOrderByOrderIndexAsc(q)))
                .toList();
    }

    @Transactional
    public QuizQuestion addQuestion(UUID contentId, QuizQuestion question) {
        Content content = contentService.findById(contentId);
        question.setContent(content);
        return questionRepository.save(question);
    }

    @Transactional
    public QuizOption addOption(UUID questionId, QuizOption option) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta", questionId));
        option.setQuestion(question);
        return optionRepository.save(option);
    }

    // answers: map questionId → selectedOptionId (null para respuesta corta)
    @Transactional
    public QuizAttempt submitAttempt(UUID contentId, UUID studentId,
                                     Map<UUID, UUID> answers, UUID assignmentId) {
        Content content = contentService.findById(contentId);
        User student = userService.findById(studentId);

        if (content.getType() != mx.bytekids.academy.entity.enums.ContentType.quiz) {
            throw new BusinessException("El contenido no es un quiz");
        }

        List<QuizQuestion> questions = questionRepository.findByContentOrderByOrderIndexAsc(content);
        Map<UUID, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));

        int totalPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
        int earnedPoints = 0;

        ContentAssignment assignment = null;
        if (assignmentId != null) {
            // Se resuelve opcionalmente
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .student(student).content(content).assignment(assignment)
                .build();
        attempt = attemptRepository.save(attempt);

        for (Map.Entry<UUID, UUID> entry : answers.entrySet()) {
            UUID questionId = entry.getKey();
            UUID optionId = entry.getValue();
            QuizQuestion question = questionMap.get(questionId);
            if (question == null) continue;

            boolean correct = false;
            QuizOption selectedOption = null;

            if (optionId != null) {
                selectedOption = optionRepository.findById(optionId).orElse(null);
                if (selectedOption != null && selectedOption.getIsCorrect()) {
                    correct = true;
                    earnedPoints += question.getPoints();
                }
            }
        }

        short score = totalPoints > 0 ? (short) ((earnedPoints * 100) / totalPoints) : 0;
        attempt.setScore(score);
        attempt = attemptRepository.save(attempt);

        if (score >= 70) {
            short xp = content.getXpReward();
            progressService.awardXp(studentId, xp, XpReason.quiz_completado,
                    attempt.getId(), "quiz_attempt", null);
            progressService.updateSubjectProgress(studentId,
                    content.getSubject() != null ? content.getSubject().getId() : null, xp);
        }
        progressService.recordDailyActivity(studentId, LocalDate.now(), 0, 0);
        return attempt;
    }
}
