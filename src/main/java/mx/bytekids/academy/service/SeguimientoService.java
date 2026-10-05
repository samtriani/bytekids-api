package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.seguimiento.SeguimientoResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ContentAssignmentRepository;
import mx.bytekids.academy.repository.QuizAttemptRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Quien necesita atencion en un salon, y por que.
 *
 * Antes el panel marcaba "Apoyo" a todo el que llevara menos de 40% del
 * temario. En un curso que dura semanas eso es TODO el grupo al principio:
 * todos en rojo, y entonces el rojo ya no le dice nada al maestro. Ahora la
 * alerta sale solo con una razon concreta y algo que hacer.
 */
@Service
@RequiredArgsConstructor
public class SeguimientoService {

    // Los umbrales, juntos y con nombre, para poder ajustarlos sin buscar.
    static final int DIAS_PARA_EMPEZAR   = 3;
    static final int DIAS_SIN_ACTIVIDAD  = 5;
    static final int DIAS_CORRECCION     = 3;
    static final int QUIZ_REPROBADO_VECES = 2;
    static final int PROMEDIO_BAJO       = 70;
    static final int MIN_CALIFICADAS     = 2;
    private static final int APRUEBA_QUIZ = 70;

    private final ComunidadService              comunidadService;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ContentAssignmentRepository   assignmentRepository;
    private final SubmissionRepository          submissionRepository;
    private final QuizAttemptRepository         attemptRepository;

    /** Lo que se sabe de un alumno, ya resumido. Es la entrada de las reglas. */
    record Hechos(OffsetDateTime inscritoEn, int hechas, int totales, OffsetDateTime ultimaActividad,
                  String corregirTitulo, OffsetDateTime corregirDesde,
                  String quizTitulo, int quizReprobado, Integer promedio, int calificadas) {}

    record Diagnostico(String estado, String razon) {}

    /**
     * Las reglas, en orden de lo que el maestro tiene que hacer primero.
     * Pura, sin base: la prueba la recorre caso por caso.
     */
    static Diagnostico diagnosticar(Hechos h, OffsetDateTime ahora) {
        if (h.hechas() == 0) {
            long dias = h.inscritoEn() == null ? DIAS_PARA_EMPEZAR : dias(h.inscritoEn(), ahora);
            if (dias < DIAS_PARA_EMPEZAR) return new Diagnostico("nuevo", "Se inscribió " + hace(dias) + ".");
            return new Diagnostico("sin_empezar", h.inscritoEn() == null
                    ? "Todavía no empieza ninguna actividad."
                    : "Se inscribió " + hace(dias) + " y no ha empezado.");
        }
        if (h.corregirDesde() != null && dias(h.corregirDesde(), ahora) >= DIAS_CORRECCION) {
            return new Diagnostico("atorado", "Tiene «" + h.corregirTitulo() + "» por corregir desde "
                    + hace(dias(h.corregirDesde(), ahora)) + ".");
        }
        if (h.quizReprobado() >= QUIZ_REPROBADO_VECES) {
            return new Diagnostico("atorado", "No ha pasado «" + h.quizTitulo() + "»: lo ha intentado "
                    + h.quizReprobado() + " veces.");
        }
        if (h.totales() > 0 && h.hechas() >= h.totales()) {
            return new Diagnostico("bien", "Terminó todo el temario.");
        }
        if (h.ultimaActividad() != null && dias(h.ultimaActividad(), ahora) >= DIAS_SIN_ACTIVIDAD) {
            return new Diagnostico("sin_actividad", "Lleva " + dias(h.ultimaActividad(), ahora)
                    + " días sin hacer actividades.");
        }
        if (h.promedio() != null && h.calificadas() >= MIN_CALIFICADAS && h.promedio() < PROMEDIO_BAJO) {
            return new Diagnostico("calificaciones_bajas", "Su promedio va en "
                    + SubmissionService.sobreDiez(h.promedio().shortValue()) + ".");
        }
        return new Diagnostico("bien", "");
    }

    private static long dias(OffsetDateTime desde, OffsetDateTime ahora) {
        return Math.max(0, ChronoUnit.DAYS.between(desde, ahora));
    }

    private static String hace(long dias) {
        return dias == 0 ? "hoy" : dias == 1 ? "hace 1 día" : "hace " + dias + " días";
    }

    @Transactional(readOnly = true)
    public SeguimientoResponse deSalon(String username, UUID salonId) {
        Classroom salon = comunidadService.exigirSalonVisible(username, salonId);
        OffsetDateTime ahora = OffsetDateTime.now();

        // Lo asignado al salon, igual que la Libreta: publicado y activo.
        List<Content> asignados = assignmentRepository.findByClassroomAndIsActiveTrue(salon).stream()
                .map(ContentAssignment::getContent)
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()) && Boolean.TRUE.equals(c.getIsActive()))
                .distinct().toList();
        Map<UUID, Content> porId = new HashMap<>();
        asignados.forEach(c -> porId.put(c.getId(), c));

        List<SeguimientoResponse.Alumno> alumnos = new ArrayList<>();
        for (ClassroomEnrollment inscripcion : enrollmentRepository.findByClassroomAndIsActiveTrue(salon)) {
            User alumno = inscripcion.getStudent();
            List<Submission> suyas = submissionRepository.findByStudentOrderBySubmittedAtDesc(alumno);
            List<QuizAttempt> intentos = attemptRepository.findByStudentOrderByCompletedAtDesc(alumno);

            // La entrega mas reciente de cada actividad del salon.
            Map<UUID, Submission> ultima = new LinkedHashMap<>();
            for (Submission s : suyas) {
                if (s.getContent() == null || !porId.containsKey(s.getContent().getId())) continue;
                if (s.getStatus() == SubmissionStatus.borrador) continue;
                ultima.putIfAbsent(s.getContent().getId(), s);
            }

            int aprobadas = 0, porCalificar = 0, calificadas = 0, suma = 0;
            String corregirTitulo = null;
            OffsetDateTime corregirDesde = null;
            for (Submission s : ultima.values()) {
                boolean esMaterial = s.getContent().getType() == ContentType.material;
                if (s.getStatus() == SubmissionStatus.aprobado) aprobadas++;
                if (s.getStatus() == SubmissionStatus.enviado) porCalificar++;
                if (s.getStatus() == SubmissionStatus.rechazado) {
                    OffsetDateTime desde = s.getReviewedAt() != null ? s.getReviewedAt() : s.getSubmittedAt();
                    if (desde != null && (corregirDesde == null || desde.isBefore(corregirDesde))) {
                        corregirDesde = desde;
                        corregirTitulo = s.getContent().getTitle();
                    }
                }
                if (!esMaterial && s.getScore() != null) { calificadas++; suma += s.getScore(); }
            }

            // Quizzes que no ha pasado: cuantas veces lo intento sin llegar.
            String quizTitulo = null;
            int quizReprobado = 0;
            Map<UUID, Integer> fallidos = new HashMap<>();
            for (QuizAttempt a : intentos) {
                if (a.getContent() == null || !porId.containsKey(a.getContent().getId())) continue;
                Submission s = ultima.get(a.getContent().getId());
                if (s != null && s.getStatus() == SubmissionStatus.aprobado) continue;
                if (a.getScore() != null && a.getScore() < APRUEBA_QUIZ) {
                    int n = fallidos.merge(a.getContent().getId(), 1, Integer::sum);
                    if (n > quizReprobado) { quizReprobado = n; quizTitulo = a.getContent().getTitle(); }
                }
            }

            // Lo ultimo que hizo el nino, en cualquier materia.
            OffsetDateTime ultimaActividad = null;
            for (Submission s : suyas) ultimaActividad = masReciente(ultimaActividad, s.getSubmittedAt());
            for (QuizAttempt a : intentos) ultimaActividad = masReciente(ultimaActividad, a.getCompletedAt());

            Integer promedio = calificadas == 0 ? null : Math.round((float) suma / calificadas);
            Diagnostico d = diagnosticar(new Hechos(inscripcion.getEnrolledAt(), ultima.size(), asignados.size(),
                    ultimaActividad, corregirTitulo, corregirDesde, quizTitulo, quizReprobado,
                    promedio, calificadas), ahora);

            String nombre = alumno.getDisplayName() != null ? alumno.getDisplayName() : alumno.getUsername();
            alumnos.add(new SeguimientoResponse.Alumno(alumno.getId(), nombre, alumno.getInitials(),
                    alumno.getAvatarUrl(), ultima.size(), aprobadas, porCalificar, promedio,
                    ultimaActividad, d.estado(), d.razon()));
        }
        alumnos.sort(Comparator.comparing(SeguimientoResponse.Alumno::nombre, String.CASE_INSENSITIVE_ORDER));
        return new SeguimientoResponse(salon.getId(), asignados.size(), alumnos);
    }

    private static OffsetDateTime masReciente(OffsetDateTime a, OffsetDateTime b) {
        if (b == null) return a;
        return a == null || b.isAfter(a) ? b : a;
    }
}
