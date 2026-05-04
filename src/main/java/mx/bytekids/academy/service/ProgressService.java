package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.XpReason;
import mx.bytekids.academy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final XpEventRepository xpEventRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final StudentSubjectProgressRepository subjectProgressRepository;
    private final UserService userService;
    private final SubjectService subjectService;

    public Integer getTotalXp(UUID studentId) {
        User student = userService.findById(studentId);
        return xpEventRepository.sumXpByStudent(student);
    }

    public int getCurrentStreak(UUID studentId) {
        User student = userService.findById(studentId);
        List<DailyActivity> activities = dailyActivityRepository.findByStudentOrderByActivityDateDesc(student);
        if (activities.isEmpty()) return 0;

        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (DailyActivity day : activities) {
            if (day.getActivityDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (day.getActivityDate().equals(LocalDate.now().minusDays(1)) && streak == 0) {
                // Permite que el streak continúe si la última actividad fue ayer
                streak++;
                expected = day.getActivityDate().minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    public List<XpEvent> getXpHistory(UUID studentId) {
        User student = userService.findById(studentId);
        return xpEventRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<StudentSubjectProgress> getSubjectProgress(UUID studentId) {
        User student = userService.findById(studentId);
        return subjectProgressRepository.findByStudentOrderByXpInSubjectDesc(student);
    }

    public List<DailyActivity> getWeekActivity(UUID studentId) {
        User student = userService.findById(studentId);
        return dailyActivityRepository.findByStudentOrderByActivityDateDesc(student)
                .stream().limit(30).toList();
    }

    @Transactional
    public void awardXp(UUID studentId, short amount, XpReason reason,
                        UUID referenceId, String referenceType, UUID awardedById) {
        User student = userService.findById(studentId);
        User awardedBy = awardedById != null ? userService.findById(awardedById) : null;

        XpEvent event = XpEvent.builder()
                .student(student).amount(amount).reason(reason)
                .referenceId(referenceId).referenceType(referenceType)
                .awardedBy(awardedBy)
                .build();
        xpEventRepository.save(event);
    }

    @Transactional
    public void updateSubjectProgress(UUID studentId, UUID subjectId, short xpEarned) {
        if (subjectId == null) return;
        User student = userService.findById(studentId);
        Subject subject = subjectService.findById(subjectId);

        StudentSubjectProgress progress = subjectProgressRepository
                .findByStudentAndSubject(student, subject)
                .orElse(StudentSubjectProgress.builder().student(student).subject(subject).build());

        progress.setXpInSubject(progress.getXpInSubject() + xpEarned);
        progress.setMissionsCompleted((short) (progress.getMissionsCompleted() + 1));

        // Nivel = xpInSubject / 200 + 1 (un nivel cada 200 XP)
        short newLevel = (short) (progress.getXpInSubject() / 200 + 1);
        progress.setLevel(newLevel);

        subjectProgressRepository.save(progress);
    }

    @Transactional
    public void recordDailyActivity(UUID studentId, LocalDate date, int missionsIncrement, int xpIncrement) {
        User student = userService.findById(studentId);
        DailyActivity activity = dailyActivityRepository
                .findByStudentAndActivityDate(student, date)
                .orElse(DailyActivity.builder().student(student).activityDate(date).build());

        activity.setMissionsCompleted((short) (activity.getMissionsCompleted() + missionsIncrement));
        activity.setXpEarned((short) (activity.getXpEarned() + xpIncrement));
        dailyActivityRepository.save(activity);
    }
}
