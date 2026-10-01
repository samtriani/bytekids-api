package mx.bytekids.academy.service;

import mx.bytekids.academy.dto.submission.ReviewRequest;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.ParentStudent;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.NotificationType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cuando el maestro califica, la familia se entera: con la calificacion sobre
 * 10, lo que dijo el maestro y una referencia al hijo para abrir sus trabajos.
 */
class SubmissionServiceFamiliaTest {

    private SubmissionRepository entregas;
    private UserService usuarios;
    private NotificationService avisos;
    private ParentStudentRepository familias;
    private XpEventRepository xp;
    private SubmissionService servicio;
    private User maestra, diego, papa;
    private Submission entrega;

    @BeforeEach
    void preparar() {
        entregas = mock(SubmissionRepository.class);
        usuarios = mock(UserService.class);
        avisos = mock(NotificationService.class);
        familias = mock(ParentStudentRepository.class);
        xp = mock(XpEventRepository.class);
        servicio = new SubmissionService(mock(DesbloqueoService.class), entregas, mock(ContentAssignmentRepository.class),
                mock(ContentService.class), usuarios, mock(ProgressService.class), mock(AchievementCheckerService.class),
                mock(ClassroomRepository.class), mock(ClassroomEnrollmentRepository.class), xp, avisos,
                mock(ClassroomService.class), familias);

        maestra = User.builder().id(UUID.randomUUID()).displayName("Maestra Ana").role(UserRole.teacher).build();
        diego = User.builder().id(UUID.randomUUID()).displayName("Diego Herrera").role(UserRole.student).build();
        papa = User.builder().id(UUID.randomUUID()).displayName("Papá de Diego").role(UserRole.parent).build();

        Content mision = new Content();
        mision.setId(UUID.randomUUID()); mision.setTitle("Misión 2"); mision.setType(ContentType.mision);
        mision.setXpReward((short) 50);
        entrega = new Submission();
        entrega.setId(UUID.randomUUID()); entrega.setStudent(diego); entrega.setContent(mision);

        when(entregas.findById(entrega.getId())).thenReturn(Optional.of(entrega));
        when(entregas.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarios.findById(maestra.getId())).thenReturn(maestra);
        when(xp.existsByReferenceIdAndReferenceType(any(), any())).thenReturn(true);
    }

    private ReviewRequest calificar(SubmissionStatus st, Integer score, String fb) {
        ReviewRequest r = new ReviewRequest();
        r.setStatus(st); r.setScore(score == null ? null : score.shortValue()); r.setFeedback(fb);
        return r;
    }

    @Test
    @SuppressWarnings("unchecked")
    void alAprobarLeAvisaALaFamiliaConLaCalificacionSobreDiezYElComentario() {
        when(familias.findByStudent(diego)).thenReturn(List.of(ParentStudent.builder().parent(papa).student(diego).build()));

        servicio.review(entrega.getId(), calificar(SubmissionStatus.aprobado, 85, "¡Muy buen trabajo!"), maestra.getId());

        ArgumentCaptor<Collection<User>> a = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(avisos).avisarATodos(a.capture(), eq(maestra), eq(NotificationType.calificacion),
                titulo.capture(), cuerpo.capture(), eq(diego.getId()), eq("trabajo_hijo"));
        assertThat(a.getValue()).containsExactly(papa);
        assertThat(titulo.getValue()).contains("Maestra Ana", "aprobó", "Misión 2", "Diego").doesNotContain("Herrera");
        assertThat(cuerpo.getValue()).contains("8.5/10", "¡Muy buen trabajo!");
    }

    @Test
    void unAjusteNoSeAnunciaComoRechazado() {
        when(familias.findByStudent(diego)).thenReturn(List.of(ParentStudent.builder().parent(papa).student(diego).build()));

        servicio.review(entrega.getId(), calificar(SubmissionStatus.rechazado, null, "Te faltó la parte 2"), maestra.getId());

        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        verify(avisos).avisarATodos(anyCollection(), any(), any(), titulo.capture(), anyString(), any(), eq("trabajo_hijo"));
        assertThat(titulo.getValue()).contains("ajuste").doesNotContainIgnoringCase("rechaz");
    }

    @Test
    void sinFamiliaLigadaNoSeAvisaANadieMas() {
        when(familias.findByStudent(diego)).thenReturn(List.of());

        servicio.review(entrega.getId(), calificar(SubmissionStatus.aprobado, 100, null), maestra.getId());

        verify(avisos, never()).avisarATodos(anyCollection(), any(), any(), anyString(), anyString(), any(), eq("trabajo_hijo"));
    }

    @Test
    void sobreDiezSinPuntoCeroColgando() {
        assertThat(SubmissionService.sobreDiez((short) 100)).isEqualTo("10");
        assertThat(SubmissionService.sobreDiez((short) 85)).isEqualTo("8.5");
        assertThat(SubmissionService.sobreDiez((short) 0)).isEqualTo("0");
    }
}
