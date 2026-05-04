package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.DailyActivity;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, UUID> {
    Optional<DailyActivity> findByStudentAndActivityDate(User student, LocalDate date);
    List<DailyActivity> findByStudentOrderByActivityDateDesc(User student);

    @Query("""
        SELECT COUNT(da) FROM DailyActivity da
        WHERE da.student = :student
          AND da.activityDate >= :from
          AND da.activityDate <= :to
        """)
    long countActiveDaysBetween(User student, LocalDate from, LocalDate to);
}
