package mx.bytekids.academy.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.bytekids.academy.entity.enums.NotificationType;

import java.util.UUID;

@Data
public class NotificationRequest {
    @NotNull(message = "El destinatario es requerido")
    private UUID recipientId;

    @NotNull(message = "El tipo es requerido")
    private NotificationType type;

    @NotBlank(message = "El título es requerido")
    private String title;

    private String body;
    private UUID referenceId;
    private String referenceType;
}
