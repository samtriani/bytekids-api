package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_achievements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "achievement_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private AchievementDefinition achievement;

    @CreationTimestamp
    @Column(name = "earned_at", updatable = false)
    private OffsetDateTime earnedAt;
}
