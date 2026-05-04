package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {
    List<XpEvent> findByStudentOrderByCreatedAtDesc(User student);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM XpEvent e WHERE e.student = :student")
    Integer sumXpByStudent(User student);
}
