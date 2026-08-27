package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.ClassSchedule;
import mx.bytekids.academy.entity.ClassSession;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    List<ClassSession> findByScheduleAndSessionDateAndIsActiveTrue(
            ClassSchedule schedule, LocalDate date);

    Optional<ClassSession> findByScheduleAndParticipantAndSessionDate(
            ClassSchedule schedule, User participant, LocalDate date);

    // Sesiones con videollamada encendida hoy (solo el maestro prende el video)
    List<ClassSession> findBySessionDateAndIsActiveTrueAndVideoActiveTrue(LocalDate date);
}
