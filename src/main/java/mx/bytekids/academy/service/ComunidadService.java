package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.comunidad.ComunidadSalonResponse;
import mx.bytekids.academy.dto.comunidad.ComunidadSalonResponse.Alumno;
import mx.bytekids.academy.dto.comunidad.ComunidadSalonResponse.Logro;
import mx.bytekids.academy.dto.comunidad.ComunidadSalonResponse.Salon;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.NotificationType;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.NotificationRepository;
import mx.bytekids.academy.repository.StudentAchievementRepository;
import mx.bytekids.academy.repository.XpEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * La Comunidad del maestro: el muro de reconocimiento de cada uno de sus
 * salones, y la posibilidad de felicitar un logro.
 *
 * QUIEN VE QUE
 * Un maestro solo ve los salones de los que es titular --la misma regla que
 * usa MessageService para saber quienes son sus alumnos--. Coordinacion y
 * direccion ven todos. Nadie ve el muro de un salon ajeno pidiendo su id.
 */
@Service
@RequiredArgsConstructor
public class ComunidadService {

    /** Referencia de la notificacion de felicitacion: marca el logro felicitado. */
    public static final String REF_FELICITACION = "felicitacion";

    private static final int LOGROS_RECIENTES = 30;

    private final UserService                   userService;
    private final ClassroomRepository           classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final XpEventRepository             xpEventRepository;
    private final StudentAchievementRepository  studentAchievementRepository;
    private final NotificationRepository        notificationRepository;
    private final NotificationService           notificationService;

    // ── Que salones puede ver ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Salon> misSalones(String username) {
        User yo = userService.findByUsername(username);
        return salonesVisibles(yo).stream()
                .map(s -> new Salon(s.getId(), s.getName(),
                        enrollmentRepository.findActiveStudentsByClassroom(s).size()))
                .sorted(Comparator.comparing(Salon::nombre))
                .toList();
    }

    private List<Classroom> salonesVisibles(User yo) {
        return esPersonal(yo)
                ? classroomRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList()
                : classroomRepository.findByTeacherAndIsActiveTrue(yo);
    }

    private static boolean esPersonal(User u) {
        return u.getRole() == UserRole.admin || u.getRole() == UserRole.director;
    }

    private Classroom salonVisible(User yo, UUID salonId) {
        Classroom salon = classroomRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salón", salonId));
        boolean suyo = esPersonal(yo)
                || (salon.getTeacher() != null && salon.getTeacher().getId().equals(yo.getId()));
        // 403 y no 404: el salon existe, lo que falta es permiso. El front
        // no cierra sesion con 403 (ver CLAUDE.md).
        if (!suyo) throw new AccessDeniedException("Ese salón no es tuyo");
        return salon;
    }

    // ── El muro de un salon ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ComunidadSalonResponse muro(String username, UUID salonId) {
        User yo = userService.findByUsername(username);
        Classroom salon = salonVisible(yo, salonId);
        List<User> alumnos = enrollmentRepository.findActiveStudentsByClassroom(salon);
        Set<UUID> idsAlumnos = new HashSet<>();
        alumnos.forEach(a -> idsAlumnos.add(a.getId()));

        // XP de los que ya tienen. La consulta devuelve solo alumnos con
        // eventos de XP: los que no aparecen aqui llevan cero.
        Map<UUID, Integer> xpDe = new HashMap<>();
        for (Object[] fila : xpEventRepository.findTopStudentsEnSalones(
                List.of(salonId), PageRequest.of(0, 1000))) {
            xpDe.put((UUID) fila[0], ((Number) fila[3]).intValue());
        }

        List<User> ordenados = new ArrayList<>(alumnos);
        ordenados.sort(Comparator.<User>comparingInt(a -> -xpDe.getOrDefault(a.getId(), 0))
                .thenComparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        List<Alumno> ranking = new ArrayList<>();
        List<Alumno> sinActividad = new ArrayList<>();
        int puesto = 0;
        for (User a : ordenados) {
            int xp = xpDe.getOrDefault(a.getId(), 0);
            Alumno fila = new Alumno(a.getId(), a.getDisplayName(), a.getInitials(), a.getAvatarUrl(),
                    xp, xp > 0 ? ++puesto : 0);
            (xp > 0 ? ranking : sinActividad).add(fila);
        }

        // Logros recientes, y cuales ya felicito este usuario.
        List<StudentAchievement> recientes = studentAchievementRepository
                .findRecientesEnSalones(List.of(salonId), PageRequest.of(0, LOGROS_RECIENTES)).stream()
                // Un alumno que se dio de baja del salon ya no es "de este salon".
                .filter(sa -> idsAlumnos.contains(sa.getStudent().getId()))
                .toList();
        Set<UUID> felicitados = recientes.isEmpty() ? Set.of()
                : new HashSet<>(notificationRepository.referenciasEnviadas(
                        yo, REF_FELICITACION, recientes.stream().map(StudentAchievement::getId).toList()));

        List<Logro> logros = recientes.stream().map(sa -> new Logro(
                sa.getId(), sa.getStudent().getId(), sa.getStudent().getDisplayName(),
                sa.getStudent().getInitials(), sa.getStudent().getAvatarUrl(),
                sa.getAchievement().getTitle(), sa.getAchievement().getIcon(),
                sa.getAchievement().getXpReward() == null ? 0 : sa.getAchievement().getXpReward(),
                sa.getEarnedAt(), felicitados.contains(sa.getId()))).toList();

        return new ComunidadSalonResponse(salon.getId(), salon.getName(), alumnos.size(),
                ranking, sinActividad, logros);
    }

    // ── Felicitar ─────────────────────────────────────────────────────────

    /**
     * Le llega al nino como notificacion: "¡Laura te felicito!". La medalla
     * la da el sistema; la felicitacion la da una persona, y eso es lo que
     * un nino de diez anos le ensena a sus papas.
     *
     * Solo se puede felicitar un logro de un alumno de un salon propio, y una
     * sola vez por persona: dos notificaciones iguales se sienten a spam.
     *
     * @return false si ya lo habia felicitado (no se manda otra)
     */
    @Transactional
    public boolean felicitar(String username, UUID logroId) {
        User yo = userService.findByUsername(username);
        StudentAchievement sa = studentAchievementRepository.findById(logroId)
                .orElseThrow(() -> new ResourceNotFoundException("Logro", logroId));
        User alumno = sa.getStudent();

        boolean esSuAlumno = esPersonal(yo) || enrollmentRepository.findByStudentAndIsActiveTrue(alumno).stream()
                .anyMatch(ins -> ins.getClassroom().getTeacher() != null
                        && ins.getClassroom().getTeacher().getId().equals(yo.getId()));
        if (!esSuAlumno) throw new AccessDeniedException("Ese alumno no es tuyo");

        if (!notificationRepository.referenciasEnviadas(yo, REF_FELICITACION, List.of(logroId)).isEmpty()) {
            return false;
        }

        String quien = primerNombre(yo.getDisplayName());
        String logro = sa.getAchievement().getTitle();
        String icono = sa.getAchievement().getIcon() == null ? "🏆" : sa.getAchievement().getIcon();
        if (logro == null || logro.isBlank()) throw new BusinessException("Ese logro no tiene nombre");

        notificationService.avisar(alumno, yo, NotificationType.logro_desbloqueado,
                "👏 ¡" + quien + " te felicitó!",
                "Por tu logro «" + logro + "» " + icono + ". ¡Sigue así!",
                logroId, REF_FELICITACION);
        return true;
    }

    /**
     * "Prof. Laura Perez" → "Prof. Laura". Con la primera palabra a secas, la
     * felicitacion decia "¡Prof. te felicitó!" y el nino no sabia de quien.
     * Un titulo se queda pegado al nombre: asi es como el nino le dice.
     */
    static String primerNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Tu maestro";
        String[] partes = nombre.trim().split("\\s+");
        boolean esTitulo = partes[0].toLowerCase()
                .matches("(prof|profa|profe|mtro|mtra|maestro|maestra|miss|lic|dr|dra|ing)\\.?");
        return esTitulo && partes.length > 1 ? partes[0] + " " + partes[1] : partes[0];
    }

    /**
     * La misma regla de "que salones puedo ver" para otras pantallas del
     * maestro (el seguimiento del panel). Una sola regla: si cada pantalla
     * calculara la suya, alguna terminaria dejando pasar un salon ajeno.
     */
    public Classroom exigirSalonVisible(String username, UUID salonId) {
        return salonVisible(userService.findByUsername(username), salonId);
    }
}
