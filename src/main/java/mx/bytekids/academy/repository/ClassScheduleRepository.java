package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.ClassSchedule;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    List<ClassSchedule> findByClassroomAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Classroom classroom);

    List<ClassSchedule> findByTeacherAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(User teacher);

    // Conflicto de maestro: mismo día, hora solapada Y rango de fechas solapado
    @Query("""
        SELECT COUNT(s) FROM ClassSchedule s
        WHERE s.teacher = :teacher
          AND s.dayOfWeek = :dayOfWeek
          AND s.isActive = true
          AND s.startTime < :endTime
          AND s.endTime   > :startTime
          AND s.startDate < :endDate
          AND s.endDate   > :startDate
        """)
    long countTeacherConflicts(@Param("teacher")   User teacher,
                               @Param("dayOfWeek") String dayOfWeek,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime")   LocalTime endTime,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate")   LocalDate endDate);

    // Conflicto de salón: mismo día, hora solapada Y rango de fechas solapado
    @Query("""
        SELECT COUNT(s) FROM ClassSchedule s
        WHERE s.classroom = :classroom
          AND s.dayOfWeek = :dayOfWeek
          AND s.isActive = true
          AND s.startTime < :endTime
          AND s.endTime   > :startTime
          AND s.startDate < :endDate
          AND s.endDate   > :startDate
        """)
    long countClassroomConflicts(@Param("classroom") Classroom classroom,
                                 @Param("dayOfWeek") String dayOfWeek,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime")   LocalTime endTime,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate")   LocalDate endDate);

    // Conflicto de alumno: algún alumno del salón tiene otra clase al mismo tiempo
    @Query("""
        SELECT COUNT(DISTINCT s)
        FROM ClassSchedule s
        JOIN ClassroomEnrollment e1 ON e1.classroom = s.classroom AND e1.isActive = true
        JOIN ClassroomEnrollment e2 ON e2.student   = e1.student  AND e2.isActive = true
        WHERE e2.classroom = :classroom
          AND s.classroom  != :classroom
          AND s.dayOfWeek  = :dayOfWeek
          AND s.isActive   = true
          AND s.startTime  < :endTime
          AND s.endTime    > :startTime
          AND s.startDate  < :endDate
          AND s.endDate    > :startDate
        """)
    long countStudentConflicts(@Param("classroom") Classroom classroom,
                               @Param("dayOfWeek") String dayOfWeek,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime")   LocalTime endTime,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate")   LocalDate endDate);
}
