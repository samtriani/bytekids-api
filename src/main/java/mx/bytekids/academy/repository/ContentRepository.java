package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {
    List<Content> findByIsActiveTrueOrderByOrderIndexAsc();
    List<Content> findByTypeAndIsActiveTrue(ContentType type);
    List<Content> findBySubjectAndIsActiveTrue(Subject subject);
    List<Content> findByCreatedByAndIsActiveTrue(User createdBy);
    List<Content> findByIsPublishedTrueAndIsActiveTrueOrderBySubjectAscOrderIndexAsc();
}
