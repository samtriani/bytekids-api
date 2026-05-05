package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.DifficultyLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ContentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private Short xpReward = 50;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.medio;

    @Column(name = "estimated_minutes")
    private Short estimatedMinutes;

    // Estructura variable por tipo de contenido almacenada como JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_body", columnDefinition = "jsonb")
    private String contentBody;

    @Column(name = "order_index")
    private Short orderIndex;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
