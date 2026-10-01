package mx.bytekids.academy.service;

import mx.bytekids.academy.dto.familia.TrabajoResponse;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Lo que una familia ve de los trabajos de su hijo: solo de SU hijo, la
 * entrega mas reciente de cada actividad, sin materiales, y los quizzes sin
 * texto.
 */
class FamiliaServiceTest {

    private UserService userService;
    private ParentStudentRepository familias;
    private SubmissionRepository entregas;
    private FamiliaService servicio;
    private User papa, victoria, otroNino;

    private static Content pieza(int orden, String titulo, ContentType tipo) {
        Content c = new Content();
        c.setId(UUID.randomUUID()); c.setOrderIndex((short) orden); c.setTitle(titulo); c.setType(tipo);
        return c;
    }

    private Submission entrega(Content c, SubmissionStatus st, String texto, int horasAtras, Integer score) {
        Submission s = new Submission();
        s.setContent(c); s.setStudent(victoria); s.setStatus(st); s.setCodeSubmitted(texto);
        s.setSubmittedAt(OffsetDateTime.now().minusHours(horasAtras));
        s.setScore(score == null ? null : score.shortValue());
        return s;
    }

    @BeforeEach
    void preparar() {
        userService = mock(UserService.class);
        familias = mock(ParentStudentRepository.class);
        entregas = mock(SubmissionRepository.class);
        servicio = new FamiliaService(userService, familias, mock(ContentService.class), entregas,
                mock(ProgressService.class), mock(StudentAchievementRepository.class), mock(CertificadoRepository.class),
                mock(ClassroomEnrollmentRepository.class), mock(ClassScheduleRepository.class), mock(JdbcTemplate.class));
        papa = User.builder().id(UUID.randomUUID()).username("papa").role(UserRole.parent).build();
        victoria = User.builder().id(UUID.randomUUID()).username("victoria").role(UserRole.student).build();
        otroNino = User.builder().id(UUID.randomUUID()).username("otro").role(UserRole.student).build();
        when(userService.findByUsername("papa")).thenReturn(papa);
        when(familias.findChildrenByParent(papa)).thenReturn(List.of(victoria));
    }

    @Test
    void noPuedeVerLosTrabajosDeUnNinoAjeno() {
        assertThatThrownBy(() -> servicio.trabajos("papa", otroNino.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(entregas);
    }

    @Test
    void veLaEntregaMasRecienteDeCadaActividadSinMaterialesYLosQuizzesSinTexto() {
        Content mision = pieza(2, "Misión 1", ContentType.mision);
        Content quiz = pieza(3, "Quiz", ContentType.quiz);
        Content material = pieza(1, "Lectura", ContentType.material);
        // Vienen de la mas nueva a la mas vieja, como las da el repositorio.
        when(entregas.findByStudentOrderBySubmittedAtDesc(victoria)).thenReturn(List.of(
                entrega(mision, SubmissionStatus.aprobado, "versión corregida", 1, 90),
                entrega(quiz, SubmissionStatus.aprobado, "Quiz contestado en la plataforma", 2, 75),
                entrega(mision, SubmissionStatus.rechazado, "primera versión", 30, null),
                entrega(material, SubmissionStatus.aprobado, "Material consultado", 40, null)));

        List<TrabajoResponse> t = servicio.trabajos("papa", victoria.getId());

        assertThat(t).extracting(TrabajoResponse::titulo).containsExactly("Misión 1", "Quiz");
        assertThat(t.get(0).texto()).isEqualTo("versión corregida");
        assertThat(t.get(0).estado()).isEqualTo("aprobada");
        assertThat(t.get(0).calificacion()).isEqualTo(90);
        assertThat(t.get(1).texto()).isNull();
    }
}
