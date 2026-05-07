package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.ClassSchedule;
import mx.bytekids.academy.entity.ClassSessionMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionMissionRepository extends JpaRepository<ClassSessionMission, UUID> {

    @Query("""
        SELECT m FROM ClassSessionMission m
        JOIN FETCH m.content c
        LEFT JOIN FETCH c.subject
        JOIN FETCH m.launchedBy
        WHERE m.schedule = :schedule AND m.sessionDate = :date
        """)
    Optional<ClassSessionMission> findByScheduleAndSessionDate(
            @Param("schedule") ClassSchedule schedule,
            @Param("date") LocalDate date);
}
