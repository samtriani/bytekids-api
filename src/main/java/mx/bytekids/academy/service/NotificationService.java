package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.dto.notification.NotificationRequest;
import mx.bytekids.academy.entity.Notification;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.NotificationType;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
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

    /**
     * Avisa a alguien. Es la puerta que usan los demás servicios cuando pasa
     * algo digno de la campanita.
     *
     * No lanza si algo sale mal: una notificación es un aviso, no el trabajo.
     * Si falla, lo que no puede pasar es que se caiga la entrega del alumno o
     * la calificación del maestro por culpa del aviso. Por eso va en su propia
     * transacción: uniéndose a la del llamador, atrapar la excepción no basta
     * --la transacción queda marcada para rollback y el commit truena después--.
     *
     * referenceType y referenceId son para que al tocarla se vaya a algún
     * lado. Sin eso la campanita informa pero no lleva a ninguna parte.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void avisar(User destinatario, User remitente, NotificationType tipo,
                       String titulo, String cuerpo,
                       UUID referenciaId, String referenciaTipo) {
        if (destinatario == null) return;
        // Nadie necesita que le avisen de lo que acaba de hacer.
        if (remitente != null && remitente.getId().equals(destinatario.getId())) return;
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(destinatario).sender(remitente)
                    .type(tipo).title(titulo).body(cuerpo)
                    .referenceId(referenciaId).referenceType(referenciaTipo)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo notificar a {}: {}", destinatario.getId(), e.getMessage());
        }
    }

    /** El mismo aviso para varias personas, sin repetidos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void avisarATodos(Collection<User> destinatarios, User remitente, NotificationType tipo,
                             String titulo, String cuerpo,
                             UUID referenciaId, String referenciaTipo) {
        Set<UUID> yaAvisados = new HashSet<>();
        for (User u : destinatarios) {
            if (u == null || !yaAvisados.add(u.getId())) continue;
            avisar(u, remitente, tipo, titulo, cuerpo, referenciaId, referenciaTipo);
        }
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
