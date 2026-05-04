package mx.bytekids.academy.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MessageRequest {
    @NotNull(message = "El destinatario es requerido")
    private UUID recipientId;

    private String subject;

    @NotBlank(message = "El mensaje no puede estar vacío")
    private String body;

    private UUID parentMessageId;
}
