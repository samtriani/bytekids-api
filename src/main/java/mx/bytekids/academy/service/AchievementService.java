package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.AchievementDefinitionRepository;
import mx.bytekids.academy.repository.StudentAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementDefinitionRepository definitionRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final UserService userService;
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
}
