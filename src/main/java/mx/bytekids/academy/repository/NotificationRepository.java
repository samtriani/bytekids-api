package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Notification;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    /** Las mas recientes primero, acotadas: el panel no necesita el historial. */
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndIsReadFalse(User recipient);

    /**
     * Tira las viejas de este usuario. Dos plazos distintos a proposito: una
     * leida ya cumplio su trabajo, una sin leer todavia puede importarle.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient = :recipient AND ("
         + "(n.isRead = true AND n.createdAt < :limiteLeidas) OR "
         + "(n.isRead = false AND n.createdAt < :limiteNoLeidas))")
    int purgarViejas(User recipient,
                     OffsetDateTime limiteLeidas,
                     OffsetDateTime limiteNoLeidas);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipient = :recipient AND n.isRead = false")
    void markAllAsRead(User recipient);
}
