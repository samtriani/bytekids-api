package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.content.AssignContentRequest;
import mx.bytekids.academy.dto.content.ContentRequest;
import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ContentAssignmentRepository;
import mx.bytekids.academy.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentAssignmentRepository assignmentRepository;
    private final SubjectService subjectService;
    private final UserService userService;
    private final ClassroomService classroomService;

    public Content findById(UUID id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", id));
    }

    public List<ContentResponse> findAll() {
        return contentRepository.findByIsPublishedTrueAndIsActiveTrueOrderBySubjectAscOrderIndexAsc()
                .stream().map(ContentResponse::from).toList();
    }

    public List<ContentResponse> findByType(ContentType type) {
        return contentRepository.findByTypeAndIsActiveTrue(type)
                .stream().map(ContentResponse::from).toList();
    }

    public List<ContentResponse> findByTeacher(UUID teacherId) {
        User teacher = userService.findById(teacherId);
        return contentRepository.findByCreatedByAndIsActiveTrue(teacher)
                .stream().map(ContentResponse::from).toList();
    }

    @Transactional
    public ContentResponse create(ContentRequest req, UUID creatorId) {
        User creator = userService.findById(creatorId);
        Subject subject = req.getSubjectId() != null ? subjectService.findById(req.getSubjectId()) : null;

        Content content = Content.builder()
                .title(req.getTitle()).description(req.getDescription())
                .type(req.getType()).subject(subject).createdBy(creator)
                .xpReward(req.getXpReward() != null ? req.getXpReward() : 50)
                .difficulty(req.getDifficulty() != null ? req.getDifficulty() : mx.bytekids.academy.entity.enums.DifficultyLevel.medio)
                .estimatedMinutes(req.getEstimatedMinutes())
                .contentBody(req.getContentBody())
                .orderIndex(req.getOrderIndex())
                .build();
        return ContentResponse.from(contentRepository.save(content));
    }

    @Transactional
    public ContentResponse update(UUID id, ContentRequest req) {
        Content content = findById(id);
        content.setTitle(req.getTitle());
        content.setDescription(req.getDescription());
        if (req.getSubjectId() != null) content.setSubject(subjectService.findById(req.getSubjectId()));
        if (req.getXpReward() != null) content.setXpReward(req.getXpReward());
        if (req.getDifficulty() != null) content.setDifficulty(req.getDifficulty());
        content.setEstimatedMinutes(req.getEstimatedMinutes());
        content.setContentBody(req.getContentBody());
        content.setOrderIndex(req.getOrderIndex());
        return ContentResponse.from(contentRepository.save(content));
    }

    @Transactional
    public ContentResponse publish(UUID id) {
        Content content = findById(id);
        content.setIsPublished(true);
        return ContentResponse.from(contentRepository.save(content));
    }

    @Transactional
    public ContentAssignment assign(UUID contentId, AssignContentRequest req, UUID assignedById) {
        if (req.getClassroomId() == null && req.getStudentId() == null) {
            throw new BusinessException("Debe especificar un salón o un alumno");
        }
        if (req.getClassroomId() != null && req.getStudentId() != null) {
            throw new BusinessException("No puede asignar a salón y alumno al mismo tiempo");
        }

        Content content = findById(contentId);
        User assignedBy = userService.findById(assignedById);
        Classroom classroom = req.getClassroomId() != null ? classroomService.findById(req.getClassroomId()) : null;
        User student = req.getStudentId() != null ? userService.findById(req.getStudentId()) : null;

        ContentAssignment assignment = ContentAssignment.builder()
                .content(content).classroom(classroom).student(student)
                .assignedBy(assignedBy).dueDate(req.getDueDate())
                .build();
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void deactivate(UUID id) {
        Content content = findById(id);
        content.setIsActive(false);
        contentRepository.save(content);
    }
}
