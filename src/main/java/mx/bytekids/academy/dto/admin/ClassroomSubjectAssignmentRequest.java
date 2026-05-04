package mx.bytekids.academy.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ClassroomSubjectAssignmentRequest {
    @NotNull(message = "El ID del salon es requerido")
    private UUID classroomId;

    @NotNull(message = "El ID de la materia es requerido")
    private UUID subjectId;

    private UUID teacherId;
}
