package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.submission.ReviewRequest;
import mx.bytekids.academy.dto.submission.SubmissionRequest;
import mx.bytekids.academy.dto.submission.SubmissionResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.ContentAssignmentRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository          submissionRepository;
    private final ContentAssignmentRepository   assignmentRepository;
    private final ContentService                contentService;
    private final UserService                   userService;
    private final ProgressService               progressService;
    private final AchievementCheckerService     achievementChecker;
    private final ClassroomRepository           classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;

    public Submission findById(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega", id));
    }

    public List<SubmissionResponse> findByStudent(UUID studentId) {
        User student = userService.findById(studentId);
        return submissionRepository.findByStudentOrderBySubmittedAtDesc(student)
                .stream().map(SubmissionResponse::from).toList();
    }

    public List<SubmissionResponse> findByContent(UUID contentId) {
        Content content = contentService.findById(contentId);
        return submissionRepository.findByContentOrderBySubmittedAtDesc(content)
                .stream().map(SubmissionResponse::from).toList();
    }

    public List<SubmissionResponse> findPending() {
        return submissionRepository.findByStatusOrderBySubmittedAtDesc(SubmissionStatus.enviado)
                .stream().map(SubmissionResponse::from).toList();
    }

    @Transactional
    public SubmissionResponse submit(SubmissionRequest req, UUID studentId) {
        User student = userService.findById(studentId);
        Content content = contentService.findById(req.getContentId());

        ContentAssignment assignment = null;
        if (req.getAssignmentId() != null) {
            assignment = assignmentRepository.findById(req.getAssignmentId()).orElse(null);
        }

        // Si ya existe una entrega, incrementa el contador de intentos
        var existing = submissionRepository.findTopByStudentAndContentOrderBySubmittedAtDesc(student, content);
        short attempts = existing.map(s -> (short) (s.getAttemptsCount() + 1)).orElse((short) 1);

        Submission submission = Submission.builder()
                .student(student).content(content).assignment(assignment)
                .codeSubmitted(req.getCodeSubmitted())
                .status(SubmissionStatus.enviado)
                .attemptsCount(attempts)
                .build();

        progressService.recordDailyActivity(studentId, LocalDate.now(), 0, 0);
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse review(UUID submissionId, ReviewRequest req, UUID reviewerId) {
        Submission submission = findById(submissionId);
        User reviewer = userService.findById(reviewerId);

        submission.setStatus(req.getStatus());
        submission.setScore(req.getScore());
        submission.setTeacherFeedback(req.getFeedback());
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setReviewedBy(reviewer);

        if (req.getStatus() == SubmissionStatus.aprobado) {
            UUID studentId  = submission.getStudent().getId();
            short xp        = submission.getContent().getXpReward();
            UUID subjectId  = submission.getContent().getSubject() != null
                    ? submission.getContent().getSubject().getId() : null;

            progressService.awardXp(studentId, xp,
                    XpReason.mision_completada, submission.getId(), "submission", reviewerId);
            progressService.updateSubjectProgress(studentId, subjectId, xp);
            progressService.recordDailyActivity(studentId, LocalDate.now(), 1, xp);
            achievementChecker.checkAndAward(studentId);
        }

        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGradebook(UUID classroomId) {
        var classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Salón", classroomId));

        List<User> students = enrollmentRepository.findActiveStudentsByClassroom(classroom);

        List<Content> contents = assignmentRepository.findByClassroomAndIsActiveTrue(classroom)
                .stream()
                .map(ContentAssignment::getContent)
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()) && Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> c.getType() != mx.bytekids.academy.entity.enums.ContentType.material)
                .distinct()
                .toList();

        Map<String, Map<String, Object>> grades = new HashMap<>();
        for (User student : students) {
            Map<String, Object> sg = new HashMap<>();
            for (Content content : contents) {
                submissionRepository.findTopByStudentAndContentOrderBySubmittedAtDesc(student, content)
                        .ifPresent(sub -> {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("score", sub.getScore());
                            entry.put("status", sub.getStatus().name());
                            entry.put("attempts", sub.getAttemptsCount());
                            entry.put("feedback", sub.getTeacherFeedback() != null ? sub.getTeacherFeedback() : "");
                            sg.put(content.getId().toString(), entry);
                        });
            }
            grades.put(student.getId().toString(), sg);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("students", students.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId().toString());
            m.put("name", s.getDisplayName() != null ? s.getDisplayName() : s.getUsername());
            m.put("initials", s.getInitials() != null ? s.getInitials() : "");
            return m;
        }).toList());
        result.put("content", contents.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId().toString());
            m.put("title", c.getTitle());
            m.put("type", c.getType().name());
            m.put("xpReward", c.getXpReward());
            m.put("dueDate", c.getDueDate() != null ? c.getDueDate().toString() : null);
            return m;
        }).toList());
        result.put("grades", grades);
        return result;
    }
}
