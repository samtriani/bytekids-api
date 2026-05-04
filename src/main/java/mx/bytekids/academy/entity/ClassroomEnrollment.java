package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "classroom_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "classroom_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassroomEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
