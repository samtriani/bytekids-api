package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.quiz.QuizQuestionResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.repository.QuizAttemptAnswerRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
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
    private final QuizAttemptAnswerRepository answerRepository;
    private final SubmissionRepository submissionRepository;
    private final ContentService contentService;
    private final UserService userService;
    private final AchievementCheckerService achievementChecker;
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
    /** Los intentos de este alumno en este quiz, del mas reciente al mas viejo. */
    public List<QuizAttempt> findAttempts(UUID contentId, UUID studentId) {
        return attemptRepository.findByStudentAndContentOrderByCompletedAtDesc(
                userService.findById(studentId), contentService.findById(contentId));
    }

    /** Que contesto en un intento, para poder repasarlo. */
    public List<QuizAttemptAnswer> findAnswers(UUID attemptId) {
        return answerRepository.findByAttempt(
                attemptRepository.findById(attemptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Intento", attemptId)));
    }

    /**
     * Deja el quiz en la libreta como cualquier otra entrega. El umbral de
     * 70 es el mismo con el que se paga el XP: aprobado si lo alcanza,
     * y si no queda pendiente de que el maestro lo vea.
     */
    private void registrarEntrega(Content content, User student, short score) {
        Submission previa = submissionRepository
                .findTopByStudentAndContentOrderBySubmittedAtDesc(student, content)
                .orElse(null);

        Submission entrega = previa != null ? previa : Submission.builder()
                .student(student).content(content).attemptsCount((short) 0).build();

        entrega.setCodeSubmitted("Quiz contestado en la plataforma");
        entrega.setScore(score);
        entrega.setStatus(score >= 70 ? SubmissionStatus.aprobado : SubmissionStatus.enviado);
        entrega.setAttemptsCount((short) ((entrega.getAttemptsCount() == null ? 0
                : entrega.getAttemptsCount()) + 1));
        submissionRepository.save(entrega);
    }

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

            // Aqui se calculaba si acertaba y se tiraba el dato: la tabla
            // quiz_attempt_answers quedaba vacia siempre. Sin esto el alumno
            // no puede repasar en que se equivoco, ni el maestro verlo.
            answerRepository.save(QuizAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .isCorrect(correct)
                    .build());
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
        // El quiz no dejaba rastro en submissions, asi que en la Libreta
        // salia "sin entregar" aunque estuviera contestado, y no contaba
        // para el avance del temario. Se registra como cualquier entrega.
        registrarEntrega(content, student, score);

        progressService.recordDailyActivity(studentId, LocalDate.now(), 0, 0);
        // Un quiz tambien suma dia de racha, asi que aqui tambien hay que
        // revisar: si no, la racha avanza y el logro no se entera.
        achievementChecker.checkAndAward(studentId);
        return attempt;
    }
}
