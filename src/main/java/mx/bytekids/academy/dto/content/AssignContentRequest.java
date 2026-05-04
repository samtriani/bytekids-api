package mx.bytekids.academy.dto.content;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AssignContentRequest {
    private UUID classroomId;   // null si es asignación individual
    private UUID studentId;     // null si es asignación a salón
    private LocalDate dueDate;
}
