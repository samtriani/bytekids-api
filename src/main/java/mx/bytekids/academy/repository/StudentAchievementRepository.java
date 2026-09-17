package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, UUID> {
    List<StudentAchievement> findByStudentOrderByEarnedAtDesc(User student);
    Optional<StudentAchievement> findByStudentAndAchievement(User student, AchievementDefinition achievement);
    boolean existsByStudentAndAchievement(User student, AchievementDefinition achievement);

    /**
     * Lo ultimo que desbloquearon los companeros de estos salones.
     *
     * Es el corazon del muro de reconocimiento: a un nino le mueve mas ver
     * que su companero logro algo que cualquier cosa que le digamos
     * nosotros. Solo dentro de su salon.
     */
    @Query("SELECT sa FROM StudentAchievement sa " +
           "WHERE sa.student.id IN (SELECT i.student.id FROM ClassroomEnrollment i " +
           "                        WHERE i.classroom.id IN :salones AND i.isActive = true) " +
           "ORDER BY sa.earnedAt DESC")
    List<StudentAchievement> findRecientesEnSalones(List<UUID> salones, Pageable pageable);
}
