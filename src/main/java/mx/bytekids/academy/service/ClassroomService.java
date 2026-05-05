package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.classroom.ClassroomRequest;
import mx.bytekids.academy.dto.classroom.ClassroomResponse;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.ClassroomEnrollment;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserService userService;

    public Classroom findById(UUID id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
    }

    public List<ClassroomResponse> findAll() {
        return classroomRepository.findByIsActiveTrueOrderByGradeLevelAscSectionAsc()
                .stream()
                .map(ClassroomResponse::from)
                .toList();
    }

    public List<ClassroomResponse> findByTeacher(UUID teacherId) {
        User teacher = userService.findById(teacherId);
        return classroomRepository.findByTeacherAndIsActiveTrue(teacher)
                .stream()
                .map(ClassroomResponse::from)
                .toList();
    }

    @Transactional
    public ClassroomResponse create(ClassroomRequest request) {
        User teacher = request.getTeacherId() != null ? userService.findById(request.getTeacherId()) : null;
        if (teacher != null) {
            validateTeacher(teacher);
        }

        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .gradeLevel(request.getGradeLevel())
                .section(request.getSection())
                .description(request.getDescription())
                .teacher(teacher)
                .schoolYear(request.getSchoolYear() != null ? request.getSchoolYear() : "2025-2026")
                .build();

        return ClassroomResponse.from(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse update(UUID id, ClassroomRequest request) {
        Classroom classroom = findById(id);
        classroom.setName(request.getName());
        classroom.setGradeLevel(request.getGradeLevel());
        classroom.setSection(request.getSection());
        classroom.setDescription(request.getDescription());
        if (request.getTeacherId() != null) {
            User teacher = userService.findById(request.getTeacherId());
            validateTeacher(teacher);
            classroom.setTeacher(teacher);
        }
        if (request.getSchoolYear() != null && !request.getSchoolYear().isBlank()) {
            classroom.setSchoolYear(request.getSchoolYear());
        }
        return ClassroomResponse.from(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse assignTeacher(UUID classroomId, UUID teacherId) {
        Classroom classroom = findById(classroomId);
        User teacher = userService.findById(teacherId);
        validateTeacher(teacher);
        classroom.setTeacher(teacher);
        return ClassroomResponse.from(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse unassignTeacher(UUID classroomId) {
        Classroom classroom = findById(classroomId);
        classroom.setTeacher(null);
        return ClassroomResponse.from(classroomRepository.save(classroom));
    }

    @Transactional
    public void enroll(UUID classroomId, UUID studentId) {
        Classroom classroom = findById(classroomId);
        User student = userService.findById(studentId);
        if (student.getRole() != UserRole.student) {
            throw new BusinessException("Solo se pueden inscribir usuarios con rol de alumno");
        }

        var existingEnrollment = enrollmentRepository.findByStudentAndClassroom(student, classroom);
        if (existingEnrollment.isPresent()) {
            ClassroomEnrollment enrollment = existingEnrollment.get();
            if (Boolean.TRUE.equals(enrollment.getIsActive())) {
                throw new BusinessException("El alumno ya esta inscrito en este salon");
            }
            enrollment.setIsActive(true);
            enrollmentRepository.save(enrollment);
            return;
        }

        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .student(student)
                .classroom(classroom)
                .build();
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void unenroll(UUID classroomId, UUID studentId) {
        Classroom classroom = findById(classroomId);
        User student = userService.findById(studentId);
        ClassroomEnrollment enrollment = enrollmentRepository.findByStudentAndClassroom(student, classroom)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada"));
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
    }

    public List<UserResponse> findStudents(UUID classroomId) {
        Classroom classroom = findById(classroomId);
        return enrollmentRepository.findActiveStudentsByClassroom(classroom)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public void deactivate(UUID classroomId) {
        Classroom classroom = findById(classroomId);
        classroom.setIsActive(false);
        classroomRepository.save(classroom);
    }

    public List<ClassroomResponse> findByStudent(UUID studentId) {
        User student = userService.findById(studentId);
        return enrollmentRepository.findByStudentAndIsActiveTrue(student)
                .stream()
                .map(e -> ClassroomResponse.from(e.getClassroom()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjects(UUID classroomId) {
        Classroom classroom = findById(classroomId);
        classroom.getSubjects().size(); // init lazy collection
        return classroom.getSubjects();
    }

    @Transactional
    public List<Subject> addSubject(UUID classroomId, UUID subjectId) {
        Classroom classroom = findById(classroomId);
        classroom.getSubjects().size();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Materia", subjectId));
        boolean exists = classroom.getSubjects().stream().anyMatch(s -> s.getId().equals(subjectId));
        if (!exists) {
            classroom.getSubjects().add(subject);
            classroomRepository.save(classroom);
        }
        return classroom.getSubjects();
    }

    @Transactional
    public void removeSubject(UUID classroomId, UUID subjectId) {
        Classroom classroom = findById(classroomId);
        classroom.getSubjects().size();
        classroom.getSubjects().removeIf(s -> s.getId().equals(subjectId));
        classroomRepository.save(classroom);
    }

    private void validateTeacher(User teacher) {
        if (teacher.getRole() != UserRole.teacher) {
            throw new BusinessException("El usuario seleccionado no es un profesor valido");
        }
    }
}
