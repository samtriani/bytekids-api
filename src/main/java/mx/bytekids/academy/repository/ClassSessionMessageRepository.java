package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.ClassSchedule;
import mx.bytekids.academy.entity.ClassSessionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ClassSessionMessageRepository extends JpaRepository<ClassSessionMessage, UUID> {

    List<ClassSessionMessage> findByScheduleAndSessionDateOrderBySentAtAsc(
            ClassSchedule schedule, LocalDate date);

    List<ClassSessionMessage> findByScheduleAndSessionDateAndSentAtAfterOrderBySentAtAsc(
            ClassSchedule schedule, LocalDate date, OffsetDateTime since);
}
