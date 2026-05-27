package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_session_missions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "session_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassSessionMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "launched_by", nullable = false)
    private User launchedBy;

    @CreationTimestamp
    @Column(name = "launched_at", updatable = false)
    private OffsetDateTime launchedAt;
}
