package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository        sessionRepo;
    private final ClassScheduleRepository       scheduleRepo;
    private final ClassSessionMissionRepository missionRepo;
    private final ClassSessionMessageRepository messageRepo;
    private final ContentService                contentService;
    private final UserService                   userService;

    private static final Map<DayOfWeek, String> DAY_MAP = Map.of(
            DayOfWeek.MONDAY,    "lunes",
            DayOfWeek.TUESDAY,   "martes",
            DayOfWeek.WEDNESDAY, "miercoles",
            DayOfWeek.THURSDAY,  "jueves",
            DayOfWeek.FRIDAY,    "viernes",
            DayOfWeek.SATURDAY,  "sabado",
            DayOfWeek.SUNDAY,    "domingo"
    );

    public Map<String, Object> getStatus(UUID scheduleId) {
        ClassSchedule s = findSchedule(scheduleId);
        boolean active = isActive(s);

        long secondsLeft = 0;
        if (active) {
            long end = s.getEndTime().toSecondOfDay();
            long now = LocalTime.now().toSecondOfDay();
            secondsLeft = Math.max(0, end - now);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("scheduleId",    scheduleId);
        result.put("active",        active);
        result.put("dayOfWeek",     s.getDayOfWeek());
        result.put("startTime",     s.getStartTime().toString());
        result.put("endTime",       s.getEndTime().toString());
        result.put("startDate",     s.getStartDate().toString());
        result.put("endDate",       s.getEndDate().toString());
        result.put("subjectName",   s.getSubject().getName());
        result.put("subjectIcon",   s.getSubject().getIcon() != null ? s.getSubject().getIcon() : "📚");
        result.put("classroomId",   s.getClassroom().getId());
        result.put("classroomName", s.getClassroom().getName());
        result.put("teacherName",   s.getTeacher().getDisplayName());
        result.put("secondsLeft",   secondsLeft);
        return result;
    }

    @Transactional
    public void join(UUID scheduleId, UUID userId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        User user = userService.findById(userId);

        if (!isActive(schedule)) {
            throw new BusinessException("La clase no está en horario activo en este momento.");
        }

        sessionRepo.findByScheduleAndParticipantAndSessionDate(schedule, user, LocalDate.now())
                .ifPresentOrElse(
                        existing -> {
                            existing.setIsActive(true);
                            existing.setLeftAt(null);
                            sessionRepo.save(existing);
                        },
                        () -> sessionRepo.save(ClassSession.builder()
                                .schedule(schedule)
                                .participant(user)
                                .sessionDate(LocalDate.now())
                                .build())
                );
    }

    @Transactional
    public boolean toggleVideo(UUID scheduleId, UUID userId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        User user = userService.findById(userId);
        ClassSession session = sessionRepo
                .findByScheduleAndParticipantAndSessionDate(schedule, user, LocalDate.now())
                .orElseThrow(() -> new BusinessException("No estás en sesión activa."));
        session.setVideoActive(!Boolean.TRUE.equals(session.getVideoActive()));
        sessionRepo.save(session);
        return Boolean.TRUE.equals(session.getVideoActive());
    }

    @Transactional
    public void leave(UUID scheduleId, UUID userId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        User user = userService.findById(userId);

        sessionRepo.findByScheduleAndParticipantAndSessionDate(schedule, user, LocalDate.now())
                .ifPresent(s -> {
                    s.setIsActive(false);
                    s.setLeftAt(OffsetDateTime.now());
                    sessionRepo.save(s);
                });
    }

    public Map<String, Object> getAttendance(UUID scheduleId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        List<ClassSession> sessions = sessionRepo.findByScheduleAndSessionDateAndIsActiveTrue(schedule, LocalDate.now());

        boolean teacherVideoActive = sessions.stream()
                .anyMatch(s -> "teacher".equals(s.getParticipant().getRole().name())
                        && Boolean.TRUE.equals(s.getVideoActive()));

        List<Map<String, Object>> participants = sessions.stream()
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId",      s.getParticipant().getId());
                    m.put("displayName", s.getParticipant().getDisplayName());
                    m.put("initials",    s.getParticipant().getInitials() != null ? s.getParticipant().getInitials() : "?");
                    m.put("role",        s.getParticipant().getRole().name());
                    m.put("joinedAt",    s.getJoinedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("participants",       participants);
        result.put("teacherVideoActive", teacherVideoActive);
        return result;
    }

    public boolean isActive(UUID scheduleId) {
        return isActive(findSchedule(scheduleId));
    }

    // ── Misión del día ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> launchMission(UUID scheduleId, UUID contentId, UUID teacherId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        Content content = contentService.findById(contentId);
        User teacher = userService.findById(teacherId);

        ClassSessionMission mission = missionRepo
                .findByScheduleAndSessionDate(schedule, LocalDate.now())
                .orElse(ClassSessionMission.builder()
                        .schedule(schedule)
                        .sessionDate(LocalDate.now())
                        .launchedBy(teacher)
                        .build());

        mission.setContent(content);
        mission.setLaunchedBy(teacher);
        missionRepo.save(mission);

        return buildMissionResponse(mission);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getCurrentMission(UUID scheduleId) {
        ClassSchedule schedule = findSchedule(scheduleId);
        return missionRepo.findByScheduleAndSessionDate(schedule, LocalDate.now())
                .map(this::buildMissionResponse);
    }

    private Map<String, Object> buildMissionResponse(ClassSessionMission m) {
        Map<String, Object> r = new HashMap<>();
        r.put("contentId",    m.getContent().getId());
        r.put("title",        m.getContent().getTitle());
        r.put("type",         m.getContent().getType().name());
        r.put("difficulty",   m.getContent().getDifficulty() != null ? m.getContent().getDifficulty().name() : "normal");
        r.put("xpReward",     m.getContent().getXpReward());
        r.put("subjectName",  m.getContent().getSubject() != null ? m.getContent().getSubject().getName() : "");
        r.put("subjectIcon",  m.getContent().getSubject() != null && m.getContent().getSubject().getIcon() != null
                ? m.getContent().getSubject().getIcon() : "📚");
        r.put("launchedAt",   m.getLaunchedAt() != null ? m.getLaunchedAt().toString() : "");
        r.put("launchedBy",   m.getLaunchedBy().getDisplayName());
        return r;
    }

    // ── Chat del aula ─────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> sendChatMessage(UUID scheduleId, UUID senderId, String content) {
        ClassSchedule schedule = findSchedule(scheduleId);
        User sender = userService.findById(senderId);
        ClassSessionMessage msg = ClassSessionMessage.builder()
                .schedule(schedule).sessionDate(LocalDate.now())
                .sender(sender).content(content.trim())
                .build();
        return toMsgMap(messageRepo.save(msg));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatMessages(UUID scheduleId, OffsetDateTime since) {
        ClassSchedule schedule = findSchedule(scheduleId);
        List<ClassSessionMessage> msgs = since != null
                ? messageRepo.findByScheduleAndSessionDateAndSentAtAfterOrderBySentAtAsc(
                        schedule, LocalDate.now(), since)
                : messageRepo.findByScheduleAndSessionDateOrderBySentAtAsc(
                        schedule, LocalDate.now());
        return msgs.stream().map(this::toMsgMap).collect(Collectors.toList());
    }

    private Map<String, Object> toMsgMap(ClassSessionMessage m) {
        Map<String, Object> r = new HashMap<>();
        r.put("id",          m.getId());
        r.put("content",     m.getContent());
        r.put("senderId",    m.getSender().getId());
        r.put("senderName",  m.getSender().getDisplayName());
        r.put("senderInit",  m.getSender().getInitials() != null ? m.getSender().getInitials() : "?");
        r.put("senderRole",  m.getSender().getRole().name());
        r.put("sentAt",      m.getSentAt() != null ? m.getSentAt().toString() : "");
        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isActive(ClassSchedule s) {
        if (!Boolean.TRUE.equals(s.getIsActive())) return false;
        if (s.getStartDate() == null || s.getEndDate() == null) return false;

        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        String todayName = DAY_MAP.getOrDefault(today.getDayOfWeek(), "");

        return s.getDayOfWeek().equalsIgnoreCase(todayName)
                && !today.isBefore(s.getStartDate())
                && !today.isAfter(s.getEndDate())
                && !now.isBefore(s.getStartTime())
                && !now.isAfter(s.getEndTime());
    }

    private ClassSchedule findSchedule(UUID id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));
    }
}
