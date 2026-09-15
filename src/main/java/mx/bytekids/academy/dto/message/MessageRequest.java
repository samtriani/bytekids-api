package mx.bytekids.academy.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class MessageRequest {
    @NotNull(message = "El destinatario es requerido")
    private UUID recipientId;

    @Size(max = 200, message = "El asunto no puede pasar de 200 caracteres")
    private String subject;

    // Sin tope, un solo mensaje podia traer megabytes de texto: el campo es
    // libre y lo llena cualquiera con sesion, incluidos los alumnos. 5000
    // caracteres son varias pantallas de texto, mas que suficiente aqui.
    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 5000, message = "El mensaje no puede pasar de 5000 caracteres")
    private String body;

    private UUID parentMessageId;
}
