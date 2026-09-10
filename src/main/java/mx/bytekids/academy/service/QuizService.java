package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.quiz.QuizQuestionResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.enums.NotificationType;
import mx.bytekids.academy.entity.enums.QuestionType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.repository.QuizAttemptAnswerRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import mx.bytekids.academy.repository.XpEventRepository;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    private final XpEventRepository xpEventRepository;
    private final ContentService contentService;
    private final UserService userService;
    private final AchievementCheckerService achievementChecker;
    private final ProgressService progressService;
    private final ClassroomService classroomService;
    private final NotificationService notificationService;

    /**
     * Preguntas con sus opciones. Antes devolvia la entidad cruda, que no tiene
     * relacion a opciones: el alumno veia la pregunta y ningun lugar donde
     * responder. Se arma el DTO para incluirlas sin filtrar cual es la correcta.
     */
    public List<QuizQuestionResponse> findQuestions(UUID contentId) {
        Content content = contentService.findById(contentId);
        return questionRepository.findByContentOrderByOrderIndexAsc(content).stream()
                .map(q -> QuizQuestionResponse.from(q, opcionesRevueltas(q)))
                .toList();
    }

    /**
     * Las opciones salen revueltas, no en el orden con el que se capturaron.
     *
     * En todos los quizzes del temario la respuesta correcta quedo en la
     * primera posicion, asi que un nino que se diera cuenta contestaba diez de
     * diez sin leer. Se arregla aqui y no con un UPDATE a la base porque el
     * problema no es el dato: es que quien captura un quiz tiende a escribir
     * primero la respuesta buena, y el siguiente que se capture va a salir
     * igual. Revolver al entregarlas lo resuelve para siempre.
     *
     * Se revuelve por peticion, asi que al repetir el quiz el orden cambia:
     * eso ademas evita que se memorice la posicion en vez del contenido.
     *
     * Verdadero/falso NO se revuelve: ahi el orden es parte de como se lee la
     * pregunta, y no hay posicion que delate nada.
     *
     * Calificar no depende del orden --submitAttempt casa por id de opcion--
     * asi que revolver es seguro.
     */
    private List<QuizOption> opcionesRevueltas(QuizQuestion pregunta) {
        List<QuizOption> opciones = new ArrayList<>(
                optionRepository.findByQuestionOrderByOrderIndexAsc(pregunta));
        if (pregunta.getQuestionType() != QuestionType.verdadero_falso) {
            Collections.shuffle(opciones);
        }
        return opciones;
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

    /**
     * El quiz se autocalifica, asi que no hay nada que revisar: el aviso es
     * para que el maestro se entere, sobre todo si le fue mal. Por eso lleva
     * la calificacion en el titulo y no un "esta esperando tu revision".
     */
    private void avisarAlMaestro(User student, Content content, Submission entrega, short score) {
        UUID materiaId = content.getSubject() != null ? content.getSubject().getId() : null;

        // score viene de 0 a 100 y en la plataforma se muestra sobre 10.
        String calificacion = String.format(java.util.Locale.US, "%.1f", score / 10.0);
        boolean aprobo = score >= 70;

        // La referencia es el SALÓN y no la entrega: la Libreta se abre por
        // salón, y un quiz de Intermedio abría la libreta de Principiante.
        for (Classroom salon : classroomService.findClassroomsForStudent(student, materiaId)) {
            notificationService.avisar(salon.getTeacher(), student, NotificationType.calificacion,
                    student.getDisplayName() + " contestó el quiz · " + calificacion,
                    content.getTitle() + (aprobo ? "" : " · no alcanzó el 7, quizá necesite apoyo"),
                    salon.getId(), "salon");
        }
    }

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
    private Submission registrarEntrega(Content content, User student, short score) {
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
        return submissionRepository.save(entrega);
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

        Submission entrega = registrarEntrega(content, student, score);

        // El XP se paga UNA vez por quiz, no una por intento. Antes la
        // referencia era el id del intento --nuevo en cada envio-- asi que
        // la verificacion de "ya se pago" nunca encontraba nada: contestar
        // el mismo quiz cinco veces daba cinco veces los puntos. Se usa la
        // entrega, que si es unica por alumno y contenido, igual que en el
        // resto de las actividades.
        boolean yaSePago = xpEventRepository
                .existsByReferenceIdAndReferenceType(entrega.getId(), "submission");
        boolean primeraAprobacion = score >= 70 && !yaSePago;
        if (primeraAprobacion) {
            short xp = content.getXpReward();
            progressService.awardXp(studentId, xp, XpReason.quiz_completado,
                    entrega.getId(), "submission", null);
            progressService.updateSubjectProgress(studentId,
                    content.getSubject() != null ? content.getSubject().getId() : null, xp);
        }

        // Solo dos avisos por quiz: cuando lo contesta por primera vez, y
        // cuando lo aprueba por primera vez. Avisar en cada reintento seria
        // ruido, y ademas dejaria colgado un "quiza necesite apoyo" de un
        // intento que el nino ya remonto.
        boolean primerIntento = entrega.getAttemptsCount() != null
                             && entrega.getAttemptsCount() == 1;
        if (primerIntento || primeraAprobacion) {
            avisarAlMaestro(student, content, entrega, score);
        }
        progressService.recordDailyActivity(studentId, LocalDate.now(), 0, 0);
        // Un quiz tambien suma dia de racha, asi que aqui tambien hay que
        // revisar: si no, la racha avanza y el logro no se entera.
        achievementChecker.checkAndAward(studentId);
        return attempt;
    }
}
