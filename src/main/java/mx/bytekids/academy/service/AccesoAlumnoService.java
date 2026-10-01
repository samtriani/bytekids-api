package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.ParentStudentRepository;
import mx.bytekids.academy.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ¿Puede quien pregunta ver los datos de ESTE alumno?
 *
 * Las rutas /progress/students/{id}/..., /achievements/students/{id} y
 * /users/{papa}/students solo revisaban el ROL: cualquier papa podia pedir
 * el XP, la racha, las materias y los logros de cualquier nino conociendo su
 * id, y los hijos de otro papa con su correo, edad y direccion. Los id son
 * dificiles de adivinar, pero eso no es una regla.
 *
 * La regla, en un solo lugar:
 *   - coordinacion y direccion: cualquiera;
 *   - un alumno: solo a si mismo;
 *   - un papa: solo a sus hijos;
 *   - un maestro: solo a los alumnos de los salones de los que es titular
 *     (la misma regla que MessageService y ComunidadService).
 */
@Service
@RequiredArgsConstructor
public class AccesoAlumnoService {

    private final UserService userService;
    private final ParentStudentRepository parentStudentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;

    /** Truena con 403 si quien pregunta no puede ver a ese alumno. */
    @Transactional(readOnly = true)
    public void exigirPuedeVer(UUID alumnoId) {
        User yo = userService.findByUsername(SecurityUtils.currentUsername());
        if (!puedeVer(yo, alumnoId)) throw new AccessDeniedException("Ese alumno no es tuyo");
    }

    @Transactional(readOnly = true)
    public boolean puedeVer(User yo, UUID alumnoId) {
        if (yo == null || alumnoId == null) return false;
        return switch (yo.getRole()) {
            case admin, director -> true;
            case student -> yo.getId().equals(alumnoId);
            case parent -> parentStudentRepository.findChildrenByParent(yo).stream()
                    .anyMatch(h -> h.getId().equals(alumnoId));
            case teacher -> {
                for (Classroom salon : classroomRepository.findByTeacherAndIsActiveTrue(yo)) {
                    if (enrollmentRepository.findActiveStudentsByClassroom(salon).stream()
                            .anyMatch(a -> a.getId().equals(alumnoId))) yield true;
                }
                yield false;
            }
        };
    }

    /**
     * Para /users/{papa}/students: un papa solo puede preguntar por SUS
     * hijos; el personal, por los de cualquiera.
     */
    @Transactional(readOnly = true)
    public void exigirEsElMismoPapa(UUID papaId) {
        User yo = userService.findByUsername(SecurityUtils.currentUsername());
        boolean personal = switch (yo.getRole()) { case admin, director -> true; default -> false; };
        if (!personal && !yo.getId().equals(papaId)) throw new AccessDeniedException("Esos no son tus hijos");
    }
}
