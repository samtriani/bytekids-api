package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classrooms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "school_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private Short gradeLevel;

    @Column(nullable = false, length = 5)
    private String section;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "school_year", nullable = false, length = 9)
    @Builder.Default
    private String schoolYear = "2025-2026";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "classroom_subjects",
        joinColumns        = @JoinColumn(name = "classroom_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id"))
    @Builder.Default
    private List<Subject> subjects = new ArrayList<>();

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
