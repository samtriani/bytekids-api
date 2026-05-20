package mx.bytekids.academy.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.DifficultyLevel;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ContentRequest {
    @NotBlank(message = "El título es requerido")
    private String title;

    private String description;

    @NotNull(message = "El tipo es requerido")
    private ContentType type;

    private UUID subjectId;
    private Short xpReward;
    private DifficultyLevel difficulty;
    private Short estimatedMinutes;
    private String contentBody;
    private Short orderIndex;
    private OffsetDateTime dueDate;
}
