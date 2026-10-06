package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Avance;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Detalle;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Fila;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Verificacion;
import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.NotificationType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Los certificados: quien puede pedirlo, quien lo entrega y quien lo ve.
 *
 * SE PIDE, NO SALE SOLO
 * Al aprobar todo, el alumno ve "Solicitar mi certificado". Eso le avisa a su
 * maestro y a coordinacion, y uno de ellos lo ENTREGA: ese es el momento de la
 * llamada de graduacion con la familia y de la invitacion al curso completo.
 * Al entregarse le llega al alumno y a sus familiares.
 *
 * CUANDO ESTA LISTO
 * Cuando tiene APROBADAS todas las actividades de la materia. Aqui si manda la
 * aprobacion del maestro, a diferencia del desbloqueo (DesbloqueoService), que
 * avanza con solo entregar: el certificado dice que el trabajo fue revisado.
 *
 * QUIEN VE QUE
 *   - El alumno, los suyos, y solo ya entregados.
 *   - Sus familiares, los de sus hijos, ya entregados.
 *   - Un maestro, los de alumnos de sus salones, entregados o no.
 *   - Coordinacion y direccion, todos.
 */
@Service
@RequiredArgsConstructor
public class CertificadoService {

    /** Sin 0/O ni 1/I/L: el folio se dicta por telefono y se escribe a mano. */
    private static final char[] ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom AZAR = new SecureRandom();

    public static final String REF_SOLICITUD = "certificado_solicitud";
    public static final String REF_CERTIFICADO = "certificado";

    private final UserService userService;
    private final ContentService contentService;
    private final SubjectService subjectService;
    private final SubmissionRepository submissionRepository;
    private final CertificadoRepository certificadoRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ── El alumno ────────────────────────────────────────────────────────

    /** Como va hacia el certificado de cada materia que lleva. */
    @Transactional(readOnly = true)
    public List<Avance> misAvances(String username) {
        User alumno = userService.findByUsername(username);
        Map<UUID, List<ContentResponse>> porMateria = contentService.findForStudent(alumno.getId()).stream()
                .filter(c -> c.getSubjectId() != null)
                .collect(Collectors.groupingBy(ContentResponse::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        Set<UUID> aprobadas = aprobadasDe(alumno);
        Map<UUID, Certificado> suyos = certificadoRepository.findByStudent(alumno).stream()
                .collect(Collectors.toMap(c -> c.getSubject().getId(), c -> c));

        List<Avance> out = new ArrayList<>();
        porMateria.forEach((materiaId, piezas) -> {
            ContentResponse una = piezas.get(0);
            int total = piezas.size();
            int hechas = (int) piezas.stream().filter(p -> aprobadas.contains(p.getId())).count();
            Certificado c = suyos.get(materiaId);
            String estado = c != null ? (c.getEntregadoEn() != null ? "entregado" : "solicitado")
                          : (hechas >= total ? "listo" : "en_curso");
            out.add(new Avance(materiaId, una.getSubjectName(), una.getSubjectColor(), una.getSubjectIcon(),
                    hechas, total, estado,
                    c != null ? c.getId() : null,
                    c != null && c.getEntregadoEn() != null ? c.getFolio() : null,
                    c != null ? c.getEntregadoEn() : null));
        });
        return out;
    }

    @Transactional
    public Avance solicitar(String username, UUID materiaId) {
        User alumno = userService.findByUsername(username);
        Avance avance = misAvances(username).stream()
                .filter(a -> a.subjectId().equals(materiaId)).findFirst()
                .orElseThrow(() -> new BusinessException("No llevas esa materia"));

        if ("en_curso".equals(avance.estado())) {
            throw new BusinessException("Todavía te faltan " + (avance.total() - avance.aprobadas())
                    + " actividades por aprobar");
        }
        if (!"listo".equals(avance.estado())) return avance;   // ya lo habia pedido: no se duplica

        Subject materia = subjectService.findById(materiaId);
        Certificado cert = certificadoRepository.save(Certificado.builder()
                .student(alumno).subject(materia).folio(nuevoFolio())
                .solicitadoEn(OffsetDateTime.now()).build());

        // Le avisa a su maestro de esa materia y a coordinacion.
        Set<User> avisar = new LinkedHashSet<>();
        for (ClassroomEnrollment ins : enrollmentRepository.findByStudentAndIsActiveTrue(alumno)) {
            Classroom salon = ins.getClassroom();
            boolean daLaMateria = salon.getSubjects() != null
                    && salon.getSubjects().stream().anyMatch(s -> s.getId().equals(materiaId));
            if (daLaMateria && salon.getTeacher() != null) avisar.add(salon.getTeacher());
        }
        avisar.addAll(userRepository.findByRoleAndIsActiveTrue(UserRole.admin));
        notificationService.avisarATodos(avisar, alumno, NotificationType.sistema,
                "🎓 " + primerNombre(alumno) + " pidió su certificado",
                alumno.getDisplayName() + " terminó «" + materia.getName()
                        + "». Entrégaselo desde Comunidad: es un gran momento para hablar con su familia.",
                cert.getId(), REF_SOLICITUD);

        return misAvances(username).stream().filter(a -> a.subjectId().equals(materiaId)).findFirst().orElse(avance);
    }

    // ── El maestro ───────────────────────────────────────────────────────

    /** Los certificados de sus alumnos: pendientes primero. */
    @Transactional(readOnly = true)
    public List<Fila> deMisAlumnos(String username) {
        User yo = userService.findByUsername(username);
        List<Certificado> lista = esPersonal(yo) ? certificadoRepository.todos()
                : certificadoRepository.deAlumnos(alumnosDe(yo));
        return lista.stream().map(c -> new Fila(c.getId(), c.getFolio(), c.getStudent().getId(),
                c.getStudent().getDisplayName(), c.getStudent().getAvatarUrl(), c.getStudent().getInitials(),
                c.getSubject().getName(), c.getSolicitadoEn(), c.getEntregadoEn())).toList();
    }

    @Transactional
    public Fila entregar(String username, UUID certificadoId) {
        User yo = userService.findByUsername(username);
        Certificado c = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado", certificadoId));
        if (!esPersonal(yo) && !alumnosDe(yo).contains(c.getStudent().getId())) {
            throw new AccessDeniedException("Ese alumno no es tuyo");
        }

        if (c.getEntregadoEn() == null) {
            c.setEntregadoEn(OffsetDateTime.now());
            c.setEntregadoPor(yo);
            certificadoRepository.save(c);

            User alumno = c.getStudent();
            String materia = c.getSubject().getName();
            notificationService.avisar(alumno, yo, NotificationType.logro_desbloqueado,
                    "🎓 ¡Tu certificado de " + materia + " está listo!",
                    "¡Felicidades, " + primerNombre(alumno) + "! Ya puedes verlo, descargarlo y enseñárselo a todos.",
                    c.getId(), REF_CERTIFICADO);
            List<User> familia = parentStudentRepository.findByStudent(alumno).stream()
                    .map(ParentStudent::getParent).toList();
            notificationService.avisarATodos(familia, yo, NotificationType.sistema,
                    "🎓 ¡" + primerNombre(alumno) + " terminó " + materia + "!",
                    "Su certificado de ByteKids Academy ya está listo. Ábrelo para verlo y descargarlo.",
                    c.getId(), REF_CERTIFICADO);
        }
        return deMisAlumnos(username).stream().filter(f -> f.id().equals(certificadoId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Certificado", certificadoId));
    }

    // ── Verlo ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Detalle detalle(String username, UUID certificadoId) {
        User yo = userService.findByUsername(username);
        Certificado c = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado", certificadoId));
        User alumno = c.getStudent();
        boolean entregado = c.getEntregadoEn() != null;

        boolean puede = switch (yo.getRole()) {
            case student -> alumno.getId().equals(yo.getId()) && entregado;
            case parent -> entregado && parentStudentRepository.findChildrenByParent(yo).stream()
                    .anyMatch(h -> h.getId().equals(alumno.getId()));
            case teacher -> alumnosDe(yo).contains(alumno.getId());
            case admin, director -> true;
        };
        if (!puede) throw new AccessDeniedException("No puedes ver ese certificado");

        // Cuanto trabajo representa: lo que aprobo de esa materia.
        UUID materiaId = c.getSubject().getId();
        Set<UUID> aprobadas = aprobadasDe(alumno);
        List<ContentResponse> piezas = contentService.findForStudent(alumno.getId()).stream()
                .filter(p -> materiaId.equals(p.getSubjectId()) && aprobadas.contains(p.getId())).toList();
        int minutos = piezas.stream().mapToInt(p -> p.getEstimatedMinutes() == null ? 0 : p.getEstimatedMinutes()).sum();
        int proyectos = (int) piezas.stream()
                .filter(p -> p.getType() == ContentType.mision || p.getType() == ContentType.proyecto).count();

        return new Detalle(c.getId(), c.getFolio(), alumno.getDisplayName(), alumno.getAvatarUrl(),
                alumno.getInitials(), c.getSubject().getName(), c.getSubject().getColor(),
                piezas.size(), minutos, proyectos, c.getSolicitadoEn(), c.getEntregadoEn(),
                c.getEntregadoPor() != null ? c.getEntregadoPor().getDisplayName() : null, entregado);
    }

    // ── Verificarlo (publico) ────────────────────────────────────────────

    /**
     * Para el QR del certificado. Solo certificados ENTREGADOS: uno pedido y
     * sin entregar no existe todavia para el mundo. Folio mal escrito o no
     * entregado: el mismo "no encontrado", para no revelar cual de los dos.
     */
    @Transactional(readOnly = true)
    public Verificacion verificar(String folio) {
        String f = folio == null ? "" : folio.trim().toUpperCase();
        Certificado c = certificadoRepository.findByFolio(f)
                .filter(x -> x.getEntregadoEn() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado", f));
        return new Verificacion(c.getFolio(), nombreConInicial(c.getStudent().getDisplayName()),
                c.getSubject().getName(), c.getEntregadoEn());
    }

    /** "Maria Zavala Ruiz" → "Maria Z." */
    static String nombreConInicial(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Alumno de ByteKids";
        String[] p = nombre.trim().split("\s+");
        return p.length == 1 ? p[0] : p[0] + " " + p[1].charAt(0) + ".";
    }

    // ── Ayudantes ────────────────────────────────────────────────────────

    private Set<UUID> aprobadasDe(User alumno) {
        return submissionRepository.findByStudentOrderBySubmittedAtDesc(alumno).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.aprobado && s.getContent() != null)
                .map(s -> s.getContent().getId())
                .collect(Collectors.toSet());
    }

    /** Los alumnos de los salones de los que es titular: la regla de siempre. */
    private Set<UUID> alumnosDe(User maestro) {
        Set<UUID> ids = new HashSet<>();
        for (Classroom salon : classroomRepository.findByTeacherAndIsActiveTrue(maestro)) {
            enrollmentRepository.findActiveStudentsByClassroom(salon).forEach(a -> ids.add(a.getId()));
        }
        return ids;
    }

    private static boolean esPersonal(User u) {
        return u.getRole() == UserRole.admin || u.getRole() == UserRole.director;
    }

    private static String primerNombre(User u) {
        String n = u.getDisplayName();
        return (n == null || n.isBlank()) ? "Tu alumno" : n.trim().split("\\s+")[0];
    }

    private String nuevoFolio() {
        for (int intento = 0; intento < 10; intento++) {
            StringBuilder sb = new StringBuilder("BK-").append(Year.now().getValue()).append('-');
            for (int i = 0; i < 5; i++) sb.append(ALFABETO[AZAR.nextInt(ALFABETO.length)]);
            if (!certificadoRepository.existsByFolio(sb.toString())) return sb.toString();
        }
        throw new BusinessException("No se pudo generar un folio; intenta de nuevo");
    }
}
