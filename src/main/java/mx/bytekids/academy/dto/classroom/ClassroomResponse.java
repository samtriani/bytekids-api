package mx.bytekids.academy.dto.classroom;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.Classroom;

import java.util.UUID;

@Data @Builder
public class ClassroomResponse {
    private UUID id;
    private String name;
    private Short gradeLevel;
    private String section;
    private String description;
    private UUID teacherId;
    private String teacherName;
    private String schoolYear;
    private Boolean isActive;

    public static ClassroomResponse from(Classroom c) {
        return ClassroomResponse.builder()
                .id(c.getId()).name(c.getName())
                .gradeLevel(c.getGradeLevel()).section(c.getSection())
                .description(c.getDescription())
                .teacherId(c.getTeacher() != null ? c.getTeacher().getId() : null)
                .teacherName(c.getTeacher() != null ? c.getTeacher().getDisplayName() : null)
                .schoolYear(c.getSchoolYear()).isActive(c.getIsActive())
                .build();
    }
}
