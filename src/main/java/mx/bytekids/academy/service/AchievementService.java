package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.AchievementDefinitionRepository;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.StudentAchievementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementDefinitionRepository definitionRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final UserService userService;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ProgressService progressService;

    public List<AchievementDefinition> findAllDefinitions() {
        return definitionRepository.findByIsActiveTrueOrderByCategoryAscRarityAsc();
    }

    public AchievementDefinition findDefinitionById(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Logro", id));
    }

    public List<StudentAchievement> findEarnedByStudent(UUID studentId) {
        User student = userService.findById(studentId);
        return studentAchievementRepository.findByStudentOrderByEarnedAtDesc(student);
    }

    @Transactional
    public StudentAchievement award(UUID studentId, UUID achievementId) {
        User student = userService.findById(studentId);
        AchievementDefinition achievement = findDefinitionById(achievementId);

        if (studentAchievementRepository.existsByStudentAndAchievement(student, achievement)) {
            throw new BusinessException("El alumno ya tiene este logro");
        }

        StudentAchievement earned = StudentAchievement.builder()
                .student(student).achievement(achievement)
                .build();
        StudentAchievement saved = studentAchievementRepository.save(earned);

        if (achievement.getXpReward() > 0) {
            progressService.awardXp(studentId, achievement.getXpReward(),
                    XpReason.logro_desbloqueado, achievement.getId(), "achievement", null);
        }
        return saved;
    }

    @Transactional
    public AchievementDefinition createDefinition(AchievementDefinition definition) {
        return definitionRepository.save(definition);
    }

    /**
     * Lo ultimo que desbloquearon los companeros de salon de este alumno.
     *
     * Devuelve solo lo que el muro necesita pintar --quien, que medalla y
     * cuando-- y no la entidad completa: un muro de reconocimiento no tiene
     * por que cargar el expediente de nadie.
     */
    public List<Map<String, Object>> recientesEnMisSalones(UUID studentId, int limit) {
        User alumno = userService.findById(studentId);
        List<UUID> salones = enrollmentRepository.findByStudentAndIsActiveTrue(alumno)
                .stream().map(i -> i.getClassroom().getId()).toList();
        if (salones.isEmpty()) return List.of();

        int tope = Math.max(1, Math.min(limit, 30));
        return studentAchievementRepository
                .findRecientesEnSalones(salones, PageRequest.of(0, tope))
                .stream()
                .map(sa -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("studentId",   sa.getStudent().getId());
                    m.put("displayName", sa.getStudent().getDisplayName());
                    m.put("initials",    sa.getStudent().getInitials());
                    m.put("esMio",       sa.getStudent().getId().equals(studentId));
                    m.put("title",       sa.getAchievement().getTitle());
                    m.put("icon",        sa.getAchievement().getIcon());
                    m.put("rarity",      sa.getAchievement().getRarity() != null
                                            ? sa.getAchievement().getRarity().name() : "comun");
                    m.put("xpReward",    sa.getAchievement().getXpReward());
                    m.put("earnedAt",    sa.getEarnedAt() != null ? sa.getEarnedAt().toString() : null);
                    return m;
                })
                .toList();
    }
}
