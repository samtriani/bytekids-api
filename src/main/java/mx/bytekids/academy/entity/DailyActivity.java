package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_activity",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "activity_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "missions_completed", nullable = false)
    @Builder.Default
    private Short missionsCompleted = 0;

    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private Short xpEarned = 0;

    @Column(name = "minutes_active", nullable = false)
    @Builder.Default
    private Short minutesActive = 0;
}
