package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.admin.ClassroomAssignmentRequest;
import mx.bytekids.academy.dto.admin.ClassroomTeacherAssignmentRequest;
import mx.bytekids.academy.dto.classroom.ClassroomRequest;
import mx.bytekids.academy.dto.classroom.ClassroomResponse;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdministratorService {

    private final UserService userService;
    private final ClassroomService classroomService;
    private final SubjectService subjectService;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        return userService.create(request);
    }

    @Transactional
    public ClassroomResponse createClassroom(ClassroomRequest request) {
        return classroomService.create(request);
    }

    @Transactional
    public Subject createSubject(Subject subject) {
        return subjectService.create(subject);
    }

    @Transactional
    public ClassroomResponse assignTeacherToClassroom(ClassroomTeacherAssignmentRequest request) {
        User teacher = userService.findById(request.getTeacherId());
        validateRole(teacher, UserRole.teacher, "profesor");
        return classroomService.assignTeacher(request.getClassroomId(), teacher.getId());
    }

    @Transactional
    public ClassroomResponse unassignTeacherFromClassroom(java.util.UUID classroomId) {
        return classroomService.unassignTeacher(classroomId);
    }

    @Transactional
    public void assignStudentToClassroom(ClassroomAssignmentRequest request) {
        User student = userService.findById(request.getStudentId());
        validateRole(student, UserRole.student, "alumno");
        classroomService.enroll(request.getClassroomId(), student.getId());
    }

    @Transactional
    public void unassignStudentFromClassroom(java.util.UUID classroomId, java.util.UUID studentId) {
        classroomService.unenroll(classroomId, studentId);
    }

    @Transactional
    public void deactivateClassroom(java.util.UUID classroomId) {
        classroomService.deactivate(classroomId);
    }

    private void validateRole(User user, UserRole expectedRole, String label) {
        if (user.getRole() != expectedRole) {
            throw new BusinessException("El usuario seleccionado no es un " + label + " valido");
        }
    }
}
