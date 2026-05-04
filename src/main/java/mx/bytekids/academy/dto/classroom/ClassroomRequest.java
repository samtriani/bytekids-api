package mx.bytekids.academy.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ClassroomRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El grado es requerido")
    private Short gradeLevel;

    @NotBlank(message = "La sección es requerida")
    private String section;

    private String description;
    private UUID teacherId;
    private String schoolYear;
}
