package mx.bytekids.academy.dto.admin;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.ClassroomSubject;

import java.util.UUID;

@Data
@Builder
public class ClassroomSubjectResponse {
    private UUID id;
    private UUID classroomId;
    private String classroomName;
    private UUID subjectId;
    private String subjectName;
    private UUID teacherId;
    private String teacherName;
    private Boolean isActive;

    public static ClassroomSubjectResponse from(ClassroomSubject classroomSubject) {
        return ClassroomSubjectResponse.builder()
                .id(classroomSubject.getId())
                .classroomId(classroomSubject.getClassroom().getId())
                .classroomName(classroomSubject.getClassroom().getName())
                .subjectId(classroomSubject.getSubject().getId())
                .subjectName(classroomSubject.getSubject().getName())
                .teacherId(classroomSubject.getTeacher() != null ? classroomSubject.getTeacher().getId() : null)
                .teacherName(classroomSubject.getTeacher() != null ? classroomSubject.getTeacher().getDisplayName() : null)
                .isActive(classroomSubject.getIsActive())
                .build();
    }
}
