package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.schedule.ClassScheduleRequest;
import mx.bytekids.academy.dto.schedule.ClassScheduleResponse;
import mx.bytekids.academy.entity.ClassSchedule;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassScheduleService {

    private static final Set<String> VALID_DAYS =
            Set.of("lunes", "martes", "miercoles", "jueves", "viernes", "sabado");

    private final ClassScheduleRepository scheduleRepository;
    private final ClassroomService classroomService;
    private final SubjectService subjectService;
    private final UserService userService;

    public List<ClassScheduleResponse> findByClassroom(UUID classroomId) {
        Classroom classroom = classroomService.findById(classroomId);
        return scheduleRepository
                .findByClassroomAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(classroom)
                .stream().map(ClassScheduleResponse::from).toList();
    }

    public List<ClassScheduleResponse> findByTeacher(UUID teacherId) {
        User teacher = userService.findById(teacherId);
        return scheduleRepository
                .findByTeacherAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(teacher)
                .stream().map(ClassScheduleResponse::from).toList();
    }

    @Transactional
    public ClassScheduleResponse create(ClassScheduleRequest req) {
        String day = req.getDayOfWeek().toLowerCase().trim();
        if (!VALID_DAYS.contains(day)) {
            throw new BusinessException("Día inválido. Use: lunes, martes, miercoles, jueves, viernes, sabado");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Classroom classroom = classroomService.findById(req.getClassroomId());
        Subject subject     = subjectService.findById(req.getSubjectId());
        User teacher        = userService.findById(req.getTeacherId());

        // Validar conflicto de maestro (mismo día, hora solapada Y rango de fechas solapado)
        long teacherConflicts = scheduleRepository.countTeacherConflicts(
                teacher, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate());
        if (teacherConflicts > 0) {
            throw new BusinessException(
                    "El maestro " + teacher.getDisplayName() +
                    " ya tiene una clase el " + day +
                    " de " + req.getStartTime() + " a " + req.getEndTime() +
                    " en ese periodo de fechas");
        }

        // Validar conflicto de salón
        long classroomConflicts = scheduleRepository.countClassroomConflicts(
                classroom, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate());
        if (classroomConflicts > 0) {
            throw new BusinessException(
                    "El salón " + classroom.getName() +
                    " ya tiene una clase el " + day +
                    " de " + req.getStartTime() + " a " + req.getEndTime() +
                    " en ese periodo de fechas");
        }

        // Validar conflicto de alumnos: algún alumno inscrito tiene otra clase al mismo tiempo
        long studentConflicts = scheduleRepository.countStudentConflicts(
                classroom, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate());
        if (studentConflicts > 0) {
            throw new BusinessException(
                    "Uno o más alumnos del salón " + classroom.getName() +
                    " ya tienen clase en otro salón el " + day +
                    " de " + req.getStartTime() + " a " + req.getEndTime() +
                    " en ese periodo. Un alumno no puede estar en dos lugares al mismo tiempo.");
        }

        ClassSchedule schedule = ClassSchedule.builder()
                .classroom(classroom).subject(subject).teacher(teacher)
                .dayOfWeek(day)
                .startTime(req.getStartTime()).endTime(req.getEndTime())
                .startDate(req.getStartDate()).endDate(req.getEndDate())
                .build();

        return ClassScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ClassScheduleResponse update(UUID id, ClassScheduleRequest req) {
        ClassSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));

        String day = req.getDayOfWeek().toLowerCase().trim();
        if (!VALID_DAYS.contains(day))
            throw new BusinessException("Día inválido.");
        if (!req.getEndTime().isAfter(req.getStartTime()))
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        if (!req.getEndDate().isAfter(req.getStartDate()))
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.");

        Classroom classroom = classroomService.findById(req.getClassroomId());
        Subject   subject   = subjectService.findById(req.getSubjectId());
        User      teacher   = userService.findById(req.getTeacherId());

        if (scheduleRepository.countTeacherConflictsExcluding(
                teacher, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate(), id) > 0)
            throw new BusinessException("El maestro " + teacher.getDisplayName() +
                    " ya tiene otra clase en ese horario y periodo.");

        if (scheduleRepository.countClassroomConflictsExcluding(
                classroom, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate(), id) > 0)
            throw new BusinessException("El salón " + classroom.getName() +
                    " ya tiene otra clase en ese horario y periodo.");

        if (scheduleRepository.countStudentConflictsExcluding(
                classroom, day, req.getStartTime(), req.getEndTime(),
                req.getStartDate(), req.getEndDate(), id) > 0)
            throw new BusinessException("Uno o más alumnos ya tienen clase en otro salón en ese horario.");

        existing.setClassroom(classroom);
        existing.setSubject(subject);
        existing.setTeacher(teacher);
        existing.setDayOfWeek(day);
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        existing.setStartDate(req.getStartDate());
        existing.setEndDate(req.getEndDate());

        return ClassScheduleResponse.from(scheduleRepository.save(existing));
    }

    @Transactional
    public void deactivate(UUID id) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));
        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
    }
}
