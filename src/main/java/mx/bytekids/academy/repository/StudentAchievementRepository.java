package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, UUID> {
    List<StudentAchievement> findByStudentOrderByEarnedAtDesc(User student);
    Optional<StudentAchievement> findByStudentAndAchievement(User student, AchievementDefinition achievement);
    boolean existsByStudentAndAchievement(User student, AchievementDefinition achievement);
}
