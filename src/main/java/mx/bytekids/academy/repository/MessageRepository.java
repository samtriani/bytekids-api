package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByRecipientOrderByCreatedAtDesc(User recipient);
    List<Message> findBySenderOrderByCreatedAtDesc(User sender);
    List<Message> findByParentMessageOrderByCreatedAtAsc(Message parent);
    long countByRecipientAndIsReadFalse(User recipient);
}
