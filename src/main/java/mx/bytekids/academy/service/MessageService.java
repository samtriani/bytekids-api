package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.message.MessageRequest;
import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    public List<Message> findInbox(UUID userId) {
        User user = userService.findById(userId);
        return messageRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public List<Message> findSent(UUID userId) {
        User user = userService.findById(userId);
        return messageRepository.findBySenderOrderByCreatedAtDesc(user);
    }

    public List<Message> findThread(UUID parentId) {
        Message parent = messageRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", parentId));
        return messageRepository.findByParentMessageOrderByCreatedAtAsc(parent);
    }

    @Transactional
    public Message send(MessageRequest req, UUID senderId) {
        User sender = userService.findById(senderId);
        User recipient = userService.findById(req.getRecipientId());
        Message parent = req.getParentMessageId() != null
                ? messageRepository.findById(req.getParentMessageId()).orElse(null) : null;

        Message message = Message.builder()
                .sender(sender).recipient(recipient)
                .subject(req.getSubject()).body(req.getBody())
                .parentMessage(parent)
                .build();
        return messageRepository.save(message);
    }

    @Transactional
    public void markAsRead(UUID messageId, UUID recipientId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", messageId));
        if (message.getRecipient().getId().equals(recipientId)) {
            message.setIsRead(true);
            message.setReadAt(OffsetDateTime.now());
            messageRepository.save(message);
        }
    }
}
