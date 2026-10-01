package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.dto.familia.HijoResponse;
import mx.bytekids.academy.dto.familia.HijoResponse.Clase;
import mx.bytekids.academy.dto.familia.HijoResponse.Logro;
import mx.bytekids.academy.dto.familia.HijoResponse.Materia;
import mx.bytekids.academy.dto.familia.HijoResponse.Paso;
import mx.bytekids.academy.dto.familia.HijoResponse.XpDia;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lo que ve una familia de sus hijos.
 *
 * Antes el panel del papa pedia cinco rutas por cada hijo, por id
 * (/progress/students/{id}/...), y armaba numeros que no eran ciertos: un
 * "progreso %" que salia de dividir el XP entre 5, un nivel con 200 XP por
 * nivel cuando el nino ve 500. Y el calendario pedia una ruta que no deja
 * entrar a papas: siempre salia vacio.
 *
 * Aqui sale todo de una vez y de las mismas fuentes que ve el nino: su feed
 * (con el desbloqueo ya calculado), sus entregas, su XP, sus logros, sus
 * certificados y sus clases. Sin ids en la ruta: devuelve los hijos de quien
 * pregunta y de nadie mas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamiliaService {

    private static final ZoneId MEXICO = ZoneId.of("America/Mexico_City");
    private static final int DIAS_XP = 70;   // diez semanas para la grafica
    private static final int LOGROS = 12;

    /** La entrega que mas cuenta gana: aprobada > en revision > por corregir. */
    private static final Map<SubmissionStatus, Integer> PESO = Map.of(
            SubmissionStatus.aprobado, 3, SubmissionStatus.enviado, 2,
            SubmissionStatus.revisado, 2, SubmissionStatus.rechazado, 1, SubmissionStatus.borrador, 0);

    private final UserService userService;
    private final ParentStudentRepository parentStudentRepository;
    private final ContentService contentService;
    private final SubmissionRepository submissionRepository;
    private final ProgressService progressService;
    private final StudentAchievementRepository studentAchievementRepository;
    private final CertificadoRepository certificadoRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * La tabla certificados se crea a mano (sql/2026-09-30_...). Si todavia
     * no existe NO se puede consultar y atrapar el error: en Postgres una
     * consulta fallida aborta la transaccion entera, y el resto del resumen
     * --y los demas hijos-- tronarian despues. Se pregunta antes, con una
     * consulta que nunca falla. Una vez que existe, se recuerda.
     */
    private volatile boolean hayCertificados = false;

    private boolean hayTablaCertificados() {
        if (hayCertificados) return true;
        Boolean existe = jdbc.queryForObject("SELECT to_regclass('public.certificados') IS NOT NULL", Boolean.class);
        hayCertificados = Boolean.TRUE.equals(existe);
        return hayCertificados;
    }

    @Transactional(readOnly = true)
    public List<HijoResponse> misHijos(String username) {
        User papa = userService.findByUsername(username);
        return parentStudentRepository.findChildrenByParent(papa).stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                .map(this::resumen)
                .toList();
    }

    private HijoResponse resumen(User hijo) {
        List<Submission> entregas = submissionRepository.findByStudentOrderBySubmittedAtDesc(hijo);
        Map<UUID, SubmissionStatus> estadoDe = new HashMap<>();
        for (Submission s : entregas) {
            if (s.getContent() == null || s.getStatus() == null) continue;
            estadoDe.merge(s.getContent().getId(), s.getStatus(),
                    (a, b) -> PESO.getOrDefault(b, 0) > PESO.getOrDefault(a, 0) ? b : a);
        }

        // ── Materias y su camino ──
        List<ContentResponse> feed = contentService.findForStudent(hijo.getId());
        Map<UUID, List<ContentResponse>> porMateria = feed.stream()
                .filter(c -> c.getSubjectId() != null)
                .collect(Collectors.groupingBy(ContentResponse::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        List<Materia> materias = new ArrayList<>();
        porMateria.forEach((materiaId, piezas) -> materias.add(materia(materiaId, piezas, estadoDe)));

        // ── Logros ──
        List<StudentAchievement> ganados = studentAchievementRepository.findByStudentOrderByEarnedAtDesc(hijo);
        List<Logro> logros = ganados.stream().limit(LOGROS).map(sa -> new Logro(
                sa.getAchievement().getTitle(), sa.getAchievement().getIcon(),
                sa.getAchievement().getXpReward() == null ? 0 : sa.getAchievement().getXpReward(),
                sa.getEarnedAt())).toList();

        // ── Certificados (la tabla puede no existir todavia) ──
        List<HijoResponse.Certificado> certificados = new ArrayList<>();
        if (hayTablaCertificados()) {
            for (mx.bytekids.academy.entity.Certificado c : certificadoRepository.findByStudent(hijo)) {
                boolean entregado = c.getEntregadoEn() != null;
                certificados.add(new HijoResponse.Certificado(entregado ? c.getId() : null, c.getSubject().getName(),
                        entregado ? "entregado" : "solicitado", entregado ? c.getFolio() : null));
            }
        }

        // ── Clases en vivo de sus salones ──
        List<Clase> clases = new ArrayList<>();
        for (ClassroomEnrollment ins : enrollmentRepository.findByStudentAndIsActiveTrue(hijo)) {
            Classroom salon = ins.getClassroom();
            for (ClassSchedule h : scheduleRepository.findByClassroomAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(salon)) {
                clases.add(new Clase(h.getDayOfWeek(), h.getStartTime(), h.getEndTime(), salon.getName(),
                        h.getSubject() != null ? h.getSubject().getName() : null));
            }
        }

        // ── XP reciente, por dia ──
        LocalDate desde = LocalDate.now(MEXICO).minusDays(DIAS_XP);
        Map<LocalDate, Integer> porDia = new TreeMap<>();
        for (XpEvent e : progressService.getXpHistory(hijo.getId())) {
            if (e.getCreatedAt() == null) continue;
            LocalDate dia = e.getCreatedAt().atZoneSameInstant(MEXICO).toLocalDate();
            if (dia.isBefore(desde)) continue;
            porDia.merge(dia, e.getAmount() == null ? 0 : e.getAmount().intValue(), Integer::sum);
        }
        List<XpDia> xpReciente = porDia.entrySet().stream().map(x -> new XpDia(x.getKey(), x.getValue())).toList();

        // ── Cuando hizo algo por ultima vez ──
        OffsetDateTime ultima = entregas.stream().map(Submission::getSubmittedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        OffsetDateTime ultimoLogro = ganados.isEmpty() ? null : ganados.get(0).getEarnedAt();
        if (ultimoLogro != null && (ultima == null || ultimoLogro.isAfter(ultima))) ultima = ultimoLogro;

        Integer xp = progressService.getTotalXp(hijo.getId());
        return new HijoResponse(hijo.getId(), hijo.getDisplayName(), hijo.getInitials(), hijo.getAvatarUrl(),
                xp == null ? 0 : xp, progressService.getCurrentStreak(hijo.getId()), ultima,
                materias, logros, certificados, clases, xpReciente);
    }

    /** Tope del texto de una entrega: un proyecto cabe de sobra. */
    private static final int MAX_TEXTO = 8000;

    /**
     * Lo que entrego un hijo: la entrega mas reciente de cada actividad, con
     * lo que escribio y lo que le dijo su maestro. Los materiales no salen
     * (solo se marcan como vistos) y los quizzes van sin texto (no tienen).
     *
     * Solo de un hijo de quien pregunta: se busca entre SUS hijos, asi que un
     * id ajeno da 403 aunque exista.
     */
    @Transactional(readOnly = true)
    public List<mx.bytekids.academy.dto.familia.TrabajoResponse> trabajos(String username, UUID hijoId) {
        User papa = userService.findByUsername(username);
        User hijo = parentStudentRepository.findChildrenByParent(papa).stream()
                .filter(h -> h.getId().equals(hijoId)).findFirst()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Ese no es tu hijo"));

        // La mas reciente de cada actividad: vienen de la mas nueva a la mas vieja.
        Map<UUID, Submission> ultima = new LinkedHashMap<>();
        for (Submission s : submissionRepository.findByStudentOrderBySubmittedAtDesc(hijo)) {
            Content c = s.getContent();
            if (c == null || c.getType() == mx.bytekids.academy.entity.enums.ContentType.material) continue;
            if (s.getStatus() == SubmissionStatus.borrador) continue;
            ultima.putIfAbsent(c.getId(), s);
        }

        return ultima.values().stream().map(s -> {
            Content c = s.getContent();
            boolean esQuiz = c.getType() == mx.bytekids.academy.entity.enums.ContentType.quiz;
            String estado = s.getStatus() == SubmissionStatus.aprobado ? "aprobada"
                          : s.getStatus() == SubmissionStatus.rechazado ? "corregir" : "revision";
            String texto = esQuiz || s.getCodeSubmitted() == null ? null
                    : (s.getCodeSubmitted().length() > MAX_TEXTO ? s.getCodeSubmitted().substring(0, MAX_TEXTO) + "…" : s.getCodeSubmitted());
            return new mx.bytekids.academy.dto.familia.TrabajoResponse(
                    c.getId(), c.getTitle(), c.getType() != null ? c.getType().name() : "mision",
                    c.getSubject() != null ? c.getSubject().getName() : null,
                    c.getSubject() != null ? c.getSubject().getColor() : null,
                    c.getOrderIndex() == null ? 0 : c.getOrderIndex(),
                    estado, s.getScore() == null ? null : s.getScore().intValue(), texto, s.getTeacherFeedback(),
                    s.getReviewedBy() != null ? s.getReviewedBy().getDisplayName() : null,
                    s.getSubmittedAt(), s.getReviewedAt());
        })
        // Por materia y en el orden del temario: como el camino.
        .sorted(Comparator.comparing((mx.bytekids.academy.dto.familia.TrabajoResponse t) -> String.valueOf(t.materia()))
                .thenComparingInt(mx.bytekids.academy.dto.familia.TrabajoResponse::orden))
        .toList();
    }

    /** El camino de una materia: la misma lectura que ve el nino en Mi Progreso. */
    private Materia materia(UUID materiaId, List<ContentResponse> piezas, Map<UUID, SubmissionStatus> estadoDe) {
        List<ContentResponse> ordenadas = piezas.stream()
                .sorted(Comparator.comparingInt(p -> p.getOrderIndex() == null ? 0 : p.getOrderIndex()))
                .toList();
        List<Paso> camino = new ArrayList<>();
        Paso siguiente = null;
        int aprobadas = 0;
        for (ContentResponse p : ordenadas) {
            SubmissionStatus st = estadoDe.get(p.getId());
            String estado;
            if (st == SubmissionStatus.aprobado) { estado = "aprobada"; aprobadas++; }
            else if (st == SubmissionStatus.rechazado) estado = "corregir";
            else if (st != null && st != SubmissionStatus.borrador) estado = "revision";
            else if (Boolean.TRUE.equals(p.getBloqueada())) estado = "bloqueada";
            else if (siguiente == null) estado = "siguiente";
            else estado = "abierta";
            Paso paso = new Paso(p.getId(), p.getOrderIndex() == null ? 0 : p.getOrderIndex(),
                    p.getTitle(), p.getType() != null ? p.getType().name() : "mision", estado);
            if ("siguiente".equals(estado)) siguiente = paso;
            camino.add(paso);
        }
        ContentResponse una = ordenadas.get(0);
        return new Materia(materiaId, una.getSubjectName(), una.getSubjectColor(), una.getSubjectIcon(),
                aprobadas, ordenadas.size(), siguiente, camino);
    }
}
