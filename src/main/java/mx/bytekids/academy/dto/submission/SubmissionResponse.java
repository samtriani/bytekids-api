package mx.bytekids.academy.dto.submission;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.enums.SubmissionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class SubmissionResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentInitials;
    private UUID contentId;
    private String contentTitle;
    private String codeSubmitted;
    private SubmissionStatus status;
    private Short score;
    private Short attemptsCount;
    private String teacherFeedback;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;

    public static SubmissionResponse from(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getDisplayName())
                .studentInitials(s.getStudent().getInitials())
                .contentId(s.getContent().getId())
                .contentTitle(s.getContent().getTitle())
                .codeSubmitted(s.getCodeSubmitted())
                .status(s.getStatus())
                .score(s.getScore())
                .attemptsCount(s.getAttemptsCount())
                .teacherFeedback(s.getTeacherFeedback())
                .submittedAt(s.getSubmittedAt())
                .reviewedAt(s.getReviewedAt())
                .build();
    }
}
