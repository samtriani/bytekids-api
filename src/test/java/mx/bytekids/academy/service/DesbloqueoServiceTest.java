package mx.bytekids.academy.service;

import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.MissionPrerequisite;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.repository.MissionPrerequisiteRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * La regla que decide si un nino avanza o se atora. Lo que importa:
 *  - sin prerrequisitos, todo abierto (los cursos de paga no cambian);
 *  - una mision se desbloquea al ENTREGARLA, no al aprobarla;
 *  - un quiz, al APROBARLO;
 *  - y entregar algo bloqueado truena aunque se entre por la URL.
 */
class DesbloqueoServiceTest {

    private MissionPrerequisiteRepository prerequisitos;
    private SubmissionRepository entregas;
    private DesbloqueoService servicio;
    private User nino;

    private static Content pieza(int orden, String titulo, ContentType tipo) {
        Content c = new Content();
        c.setId(UUID.randomUUID());
        c.setOrderIndex((short) orden);
        c.setTitle(titulo);
        c.setType(tipo);
        return c;
    }

    private static MissionPrerequisite requiere(Content mision, Content antes) {
        return MissionPrerequisite.builder().mission(mision).prerequisite(antes).build();
    }

    private Submission entrega(Content c, SubmissionStatus estado) {
        Submission s = new Submission();
        s.setContent(c);
        s.setStudent(nino);
        s.setStatus(estado);
        return s;
    }

    @BeforeEach
    void preparar() {
        prerequisitos = mock(MissionPrerequisiteRepository.class);
        entregas = mock(SubmissionRepository.class);
        servicio = new DesbloqueoService(prerequisitos, entregas);
        nino = User.builder().id(UUID.randomUUID()).username("diego").build();
        when(entregas.findByStudentOrderBySubmittedAtDesc(nino)).thenReturn(List.of());
    }

    @Test
    void sinPrerrequisitosTodoEstaAbierto() {
        Content suelta = pieza(3, "Actividad de un curso de paga", ContentType.mision);
        when(prerequisitos.conRequisitos(any())).thenReturn(List.of());
        assertThat(servicio.bloqueadas(nino, List.of(suelta))).isEmpty();
    }

    @Test
    void laMisionSeDesbloqueaAlEntregarNoAlAprobar() {
        Content mision1 = pieza(2, "Misión 1", ContentType.mision);
        Content quiz = pieza(3, "Quiz", ContentType.quiz);
        when(prerequisitos.conRequisitos(any())).thenReturn(List.of(requiere(quiz, mision1)));

        // Sin entregar: bloqueado, y dice que le falta.
        assertThat(servicio.loQueFalta(nino, quiz)).contains(mision1);

        // Entregada y esperando al maestro: ya avanza.
        when(entregas.findByStudentOrderBySubmittedAtDesc(nino))
                .thenReturn(List.of(entrega(mision1, SubmissionStatus.enviado)));
        assertThat(servicio.loQueFalta(nino, quiz)).isEmpty();

        // Con correcciones pedidas tambien avanza: vuelve despues a corregir.
        when(entregas.findByStudentOrderBySubmittedAtDesc(nino))
                .thenReturn(List.of(entrega(mision1, SubmissionStatus.rechazado)));
        assertThat(servicio.loQueFalta(nino, quiz)).isEmpty();
    }

    @Test
    void elQuizSeDesbloqueaAlAprobarlo() {
        Content quiz = pieza(3, "Quiz", ContentType.quiz);
        Content material = pieza(4, "Material", ContentType.material);
        when(prerequisitos.conRequisitos(any())).thenReturn(List.of(requiere(material, quiz)));

        // Contestado pero reprobado: sigue bloqueado.
        when(entregas.findByStudentOrderBySubmittedAtDesc(nino))
                .thenReturn(List.of(entrega(quiz, SubmissionStatus.enviado)));
        assertThat(servicio.loQueFalta(nino, material)).contains(quiz);

        when(entregas.findByStudentOrderBySubmittedAtDesc(nino))
                .thenReturn(List.of(entrega(quiz, SubmissionStatus.aprobado)));
        assertThat(servicio.loQueFalta(nino, material)).isEmpty();
    }

    @Test
    void entregarAlgoBloqueadoTruenaConElNombreDeLoQueFalta() {
        Content mision1 = pieza(2, "Misión 1: Entrevista a una IA", ContentType.mision);
        Content quiz = pieza(3, "Quiz", ContentType.quiz);
        when(prerequisitos.conRequisitos(any())).thenReturn(List.of(requiere(quiz, mision1)));

        assertThatThrownBy(() -> servicio.exigirDesbloqueada(nino, quiz))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Misión 1: Entrevista a una IA");
    }

    @Test
    void conVariosFaltantesSeñalaElPrimeroDelTemario() {
        Content p1 = pieza(1, "Pieza 1", ContentType.material);
        Content p2 = pieza(2, "Pieza 2", ContentType.mision);
        Content p3 = pieza(3, "Pieza 3", ContentType.mision);
        when(prerequisitos.conRequisitos(any()))
                .thenReturn(List.of(requiere(p3, p2), requiere(p3, p1)));
        assertThat(servicio.loQueFalta(nino, p3)).contains(p1);
    }
}
