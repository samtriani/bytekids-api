package mx.bytekids.academy.dto.content;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.DifficultyLevel;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class ContentResponse {
    private UUID id;
    private String title;
    private String description;
    private ContentType type;
    private UUID subjectId;
    private String subjectName;
    private String subjectIcon;
    private UUID createdById;
    private String createdByName;
    private Short xpReward;
    private DifficultyLevel difficulty;
    private Short estimatedMinutes;
    private String contentBody;
    private Short orderIndex;
    private Boolean isPublished;
    private OffsetDateTime createdAt;

    public static ContentResponse from(Content c) {
        return ContentResponse.builder()
                .id(c.getId()).title(c.getTitle()).description(c.getDescription())
                .type(c.getType())
                .subjectId(c.getSubject() != null ? c.getSubject().getId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .subjectIcon(c.getSubject() != null ? c.getSubject().getIcon() : null)
                .createdById(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getDisplayName())
                .xpReward(c.getXpReward()).difficulty(c.getDifficulty())
                .estimatedMinutes(c.getEstimatedMinutes()).contentBody(c.getContentBody())
                .orderIndex(c.getOrderIndex()).isPublished(c.getIsPublished())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
