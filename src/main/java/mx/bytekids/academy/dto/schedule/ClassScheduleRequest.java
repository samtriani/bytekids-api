package mx.bytekids.academy.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class ClassScheduleRequest {
    @NotNull(message = "El salón es requerido")
    private UUID classroomId;

    @NotNull(message = "La materia es requerida")
    private UUID subjectId;

    @NotNull(message = "El maestro es requerido")
    private UUID teacherId;

    @NotBlank(message = "El día de la semana es requerido")
    private String dayOfWeek;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime endTime;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate endDate;
}
