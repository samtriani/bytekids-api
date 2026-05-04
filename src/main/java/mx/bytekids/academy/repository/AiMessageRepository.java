package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.AiConversation;
import mx.bytekids.academy.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findByConversationOrderByCreatedAtAsc(AiConversation conversation);
}
