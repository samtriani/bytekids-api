package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.XpEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {
    List<XpEvent> findByStudentOrderByCreatedAtDesc(User student);

    /** Para no volver a otorgar XP por algo que ya se pago. */
    boolean existsByReferenceIdAndReferenceType(UUID referenceId, String referenceType);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM XpEvent e WHERE e.student = :student")
    Integer sumXpByStudent(User student);

    @Query("SELECT e.student.id, e.student.displayName, e.student.initials, SUM(e.amount) " +
           "FROM XpEvent e WHERE e.student.role = 'student' " +
           "GROUP BY e.student.id, e.student.displayName, e.student.initials " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> findTopStudents(Pageable pageable);

    /**
     * Los mejores DENTRO de unos salones. El ranking que ve un alumno tiene
     * que ser el de su grupo: el global le ensena nombres completos de
     * menores de otras clases, que ademas no le dicen nada.
     */
    @Query("SELECT e.student.id, e.student.displayName, e.student.initials, SUM(e.amount) " +
           "FROM XpEvent e WHERE e.student.role = 'student' " +
           "AND e.student.id IN (SELECT i.student.id FROM ClassroomEnrollment i " +
           "                     WHERE i.classroom.id IN :salones AND i.isActive = true) " +
           "GROUP BY e.student.id, e.student.displayName, e.student.initials " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> findTopStudentsEnSalones(List<UUID> salones, Pageable pageable);
}
