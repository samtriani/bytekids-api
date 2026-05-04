package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.notification.NotificationRequest;
import mx.bytekids.academy.entity.Notification;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public List<Notification> findByRecipient(UUID recipientId) {
        User recipient = userService.findById(recipientId);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long countUnread(UUID recipientId) {
        User recipient = userService.findById(recipientId);
        return notificationRepository.countByRecipientAndIsReadFalse(recipient);
    }

    @Transactional
    public Notification send(NotificationRequest req, UUID senderId) {
        User recipient = userService.findById(req.getRecipientId());
        User sender = senderId != null ? userService.findById(senderId) : null;

        Notification notification = Notification.builder()
                .recipient(recipient).sender(sender)
                .type(req.getType()).title(req.getTitle()).body(req.getBody())
                .referenceId(req.getReferenceId()).referenceType(req.getReferenceType())
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", notificationId));
        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new mx.bytekids.academy.exception.UnauthorizedException("No puedes marcar esta notificación");
        }
        notification.setIsRead(true);
        notification.setReadAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        User recipient = userService.findById(recipientId);
        notificationRepository.markAllAsRead(recipient);
    }
}
