package mx.bytekids.academy.dto.submission;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmissionRequest {
    @NotNull(message = "El contenido es requerido")
    private UUID contentId;

    private UUID assignmentId;
    private String codeSubmitted;
}
