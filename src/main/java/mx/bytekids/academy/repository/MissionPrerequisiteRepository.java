package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.MissionPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MissionPrerequisiteRepository extends JpaRepository<MissionPrerequisite, UUID> {
    List<MissionPrerequisite> findByMission(Content mission);

    @Query("""
        SELECT mp.prerequisite FROM MissionPrerequisite mp
        WHERE mp.mission = :mission
        """)
    List<Content> findPrerequisitesByMission(Content mission);
}
