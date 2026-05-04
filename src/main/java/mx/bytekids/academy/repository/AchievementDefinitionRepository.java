package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {
    List<AchievementDefinition> findByIsActiveTrueOrderByCategoryAscRarityAsc();
}
