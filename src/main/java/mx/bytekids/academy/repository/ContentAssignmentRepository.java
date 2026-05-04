package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.ContentAssignment;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ContentAssignmentRepository extends JpaRepository<ContentAssignment, UUID> {
    List<ContentAssignment> findByClassroomAndIsActiveTrue(Classroom classroom);
    List<ContentAssignment> findByStudentAndIsActiveTrue(User student);
    List<ContentAssignment> findByContentAndIsActiveTrue(Content content);

    @Query("""
        SELECT ca FROM ContentAssignment ca
        WHERE ca.isActive = true
          AND (ca.student = :student
               OR ca.classroom IN (
                   SELECT ce.classroom FROM ClassroomEnrollment ce
                   WHERE ce.student = :student AND ce.isActive = true
               ))
        ORDER BY ca.assignedAt DESC
        """)
    List<ContentAssignment> findAllAssignmentsForStudent(User student);
}
