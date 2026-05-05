package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.bytekids.academy.entity.enums.AchievementCategory;
import mx.bytekids.academy.entity.enums.RarityLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "achievement_definitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AchievementDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 10)
    private String icon;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private Short xpReward = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AchievementCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private RarityLevel rarity = RarityLevel.comun;

    @Column(name = "condition_type", length = 50)
    private String conditionType;

    // { "count": 10 } | { "days": 7 } | { "subject": "python", "level": 5 }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_value", columnDefinition = "jsonb")
    private String conditionValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
