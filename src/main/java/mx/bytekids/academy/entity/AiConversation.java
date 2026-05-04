package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import mx.bytekids.academy.entity.enums.AiContextType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false)
    @Builder.Default
    private AiContextType contextType = AiContextType.general;

    @Column(name = "context_id")
    private UUID contextId;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "last_message_at", nullable = false)
    private OffsetDateTime lastMessageAt;

    @Column(name = "message_count", nullable = false)
    @Builder.Default
    private Short messageCount = 0;
}
