package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mission_prerequisites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"mission_id", "prerequisite_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MissionPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Content mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Content prerequisite;
}
