package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.ClassroomEnrollment;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomEnrollmentRepository extends JpaRepository<ClassroomEnrollment, UUID> {
    List<ClassroomEnrollment> findByClassroomAndIsActiveTrue(Classroom classroom);
    List<ClassroomEnrollment> findByStudentAndIsActiveTrue(User student);
    Optional<ClassroomEnrollment> findByStudentAndClassroom(User student, Classroom classroom);

    @Query("SELECT e.student FROM ClassroomEnrollment e WHERE e.classroom = :classroom AND e.isActive = true")
    List<User> findActiveStudentsByClassroom(Classroom classroom);
}
