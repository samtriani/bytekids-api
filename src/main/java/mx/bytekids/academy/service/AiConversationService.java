package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.AiContextType;
import mx.bytekids.academy.entity.enums.MessageRole;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.AiConversationRepository;
import mx.bytekids.academy.repository.AiMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserService userService;

    public List<AiConversation> findByStudent(UUID studentId) {
        User student = userService.findById(studentId);
        return conversationRepository.findByStudentOrderByLastMessageAtDesc(student);
    }

    public AiConversation findById(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación", id));
    }

    public List<AiMessage> findMessages(UUID conversationId) {
        AiConversation conversation = findById(conversationId);
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional
    public AiConversation startConversation(UUID studentId, AiContextType contextType, UUID contextId) {
        User student = userService.findById(studentId);
        AiConversation conversation = AiConversation.builder()
                .student(student).contextType(contextType).contextId(contextId)
                .lastMessageAt(OffsetDateTime.now())
                .build();
        return conversationRepository.save(conversation);
    }

    @Transactional
    public AiMessage addMessage(UUID conversationId, MessageRole role, String content) {
        AiConversation conversation = findById(conversationId);
        AiMessage message = AiMessage.builder()
                .conversation(conversation).role(role).content(content)
                .build();
        conversation.setLastMessageAt(OffsetDateTime.now());
        conversation.setMessageCount((short) (conversation.getMessageCount() + 1));
        conversationRepository.save(conversation);
        return messageRepository.save(message);
    }
}
