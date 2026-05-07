package mx.bytekids.academy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.repository.AchievementDefinitionRepository;
import mx.bytekids.academy.repository.StudentAchievementRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementCheckerService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AchievementDefinitionRepository definitionRepo;
    private final StudentAchievementRepository    studentAchievementRepo;
    private final AchievementService              achievementService;
    private final SubmissionRepository            submissionRepo;
    private final ProgressService                 progressService;
    private final UserService                     userService;

    /**
     * Evalúa todas las definiciones de logros no obtenidas por el alumno
     * y otorga las que ya cumplen su condición.
     * Llamar después de cualquier acción que pueda desbloquear logros:
     * aprobación de entrega, inicio de racha, etc.
     */
    @Transactional
    public void checkAndAward(UUID studentId) {
        User student = userService.findById(studentId);

        // IDs de logros ya ganados — evita duplicados
        Set<UUID> alreadyEarned = studentAchievementRepo
                .findByStudentOrderByEarnedAtDesc(student).stream()
                .map(sa -> sa.getAchievement().getId())
                .collect(Collectors.toSet());

        List<AchievementDefinition> candidates = definitionRepo
                .findByIsActiveTrueOrderByCategoryAscRarityAsc().stream()
                .filter(d -> !alreadyEarned.contains(d.getId()))
                .filter(d -> d.getConditionType() != null && d.getConditionValue() != null)
                .toList();

        if (candidates.isEmpty()) return;

        // Carga estadísticas del alumno una sola vez
        long   approvedCount = submissionRepo.countApprovedByStudent(student);
        Integer totalXp      = null;
        Integer streak       = null;

        for (AchievementDefinition def : candidates) {
            try {
                boolean met = evaluate(def, student, studentId, approvedCount,
                        totalXp, streak);

                // Actualiza caché lazy de métricas costosas si ya las calculamos
                if (def.getConditionType().equals("streak_days") && streak == null)
                    streak = progressService.getCurrentStreak(studentId);
                if (def.getConditionType().equals("xp_total") && totalXp == null)
                    totalXp = progressService.getTotalXp(studentId);

                if (met) {
                    achievementService.award(studentId, def.getId());
                    log.info("🏆 Logro desbloqueado: '{}' para alumno {}", def.getTitle(), studentId);
                }
            } catch (Exception e) {
                log.warn("No se pudo evaluar logro '{}': {}", def.getTitle(), e.getMessage());
            }
        }
    }

    private boolean evaluate(AchievementDefinition def, User student, UUID studentId,
                             long approvedCount, Integer totalXp, Integer streak) throws Exception {
        JsonNode cond = MAPPER.readTree(def.getConditionValue());

        return switch (def.getConditionType()) {

            case "missions_count" ->
                approvedCount >= cond.path("count").asLong(0);

            case "streak_days" -> {
                int currentStreak = progressService.getCurrentStreak(studentId);
                yield currentStreak >= cond.path("days").asInt(0);
            }

            case "xp_total" -> {
                int xp = progressService.getTotalXp(studentId);
                yield xp >= cond.path("amount").asInt(0);
            }

            case "subject_missions" -> {
                String subjectName = cond.path("subject").asText("");
                int required = cond.path("count").asInt(0);
                yield submissionRepo.countApprovedByStudentAndSubject(
                        student, SubmissionStatus.aprobado, subjectName) >= required;
            }

            case "subject_level" -> {
                String subjectName = cond.path("subject").asText("");
                int requiredLevel  = cond.path("level").asInt(0);
                yield progressService.getSubjectProgress(studentId).stream()
                        .filter(sp -> subjectName.equalsIgnoreCase(sp.getSubject().getName()))
                        .anyMatch(sp -> sp.getLevel() >= requiredLevel);
            }

            case "project_count" ->
                submissionRepo.countApprovedByStudentAndType(
                        student, SubmissionStatus.aprobado, ContentType.proyecto)
                        >= cond.path("count").asLong(0);

            default -> false;
        };
    }
}
