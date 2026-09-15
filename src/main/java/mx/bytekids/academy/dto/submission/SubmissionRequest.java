package mx.bytekids.academy.dto.submission;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmissionRequest {
    @NotNull(message = "El contenido es requerido")
    private UUID contentId;

    private UUID assignmentId;

    // Sin tope, la entrega de un alumno podia traer megabytes: es un campo de
    // texto libre y del otro lado hay ninos, que a veces se recargan en una
    // tecla. Eso llega entero a la Libreta del maestro, que pinta la entrega
    // completa en el modal de calificar. 10000 caracteres son varias cuartillas
    // --de sobra para cualquier respuesta real-- y acotan lo que esa pantalla
    // puede recibir.
    @Size(max = 10000, message = "La entrega no puede pasar de 10000 caracteres")
    private String codeSubmitted;
}
