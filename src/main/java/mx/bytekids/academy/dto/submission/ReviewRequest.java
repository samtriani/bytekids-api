package mx.bytekids.academy.dto.submission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.bytekids.academy.entity.enums.SubmissionStatus;

@Data
public class ReviewRequest {
    @NotNull
    private SubmissionStatus status;   // aprobado | rechazado | revisado

    @Min(0) @Max(100)
    private Short score;

    private String feedback;
}
