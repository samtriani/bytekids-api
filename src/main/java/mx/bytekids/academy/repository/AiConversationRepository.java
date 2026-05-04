package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.AiConversation;
import mx.bytekids.academy.entity.AiMessage;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findByStudentOrderByLastMessageAtDesc(User student);
}
