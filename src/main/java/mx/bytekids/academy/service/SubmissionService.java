package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.submission.ReviewRequest;
import mx.bytekids.academy.dto.submission.SubmissionRequest;
import mx.bytekids.academy.dto.submission.SubmissionResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ContentAssignmentRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ContentAssignmentRepository assignmentRepository;
    private final ContentService contentService;
    private final UserService userService;
    private final ProgressService progressService;

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
            short xp = submission.getContent().getXpReward();
            progressService.awardXp(submission.getStudent().getId(), xp,
                    XpReason.mision_completada, submission.getId(), "submission", reviewerId);
            progressService.updateSubjectProgress(
                    submission.getStudent().getId(),
                    submission.getContent().getSubject().getId(), xp);
            progressService.recordDailyActivity(
                    submission.getStudent().getId(), LocalDate.now(), 1, xp);
        }

        return SubmissionResponse.from(submissionRepository.save(submission));
    }
}
