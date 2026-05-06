package mx.bytekids.academy.dto.schedule;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.ClassSchedule;

import java.time.LocalTime;
import java.util.UUID;

@Data @Builder
public class ClassScheduleResponse {
    private UUID id;
    private UUID classroomId;
    private String classroomName;
    private UUID subjectId;
    private String subjectName;
    private String subjectIcon;
    private UUID teacherId;
    private String teacherName;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public static ClassScheduleResponse from(ClassSchedule s) {
        return ClassScheduleResponse.builder()
                .id(s.getId())
                .classroomId(s.getClassroom().getId())
                .classroomName(s.getClassroom().getName())
                .subjectId(s.getSubject().getId())
                .subjectName(s.getSubject().getName())
                .subjectIcon(s.getSubject().getIcon())
                .teacherId(s.getTeacher().getId())
                .teacherName(s.getTeacher().getDisplayName())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .build();
    }
}
