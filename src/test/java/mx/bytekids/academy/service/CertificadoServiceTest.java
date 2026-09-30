package mx.bytekids.academy.service;

import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Las reglas del certificado: no se pide sin terminar, el alumno y su familia
 * no lo ven antes de que se entregue, y ningun maestro ve el de un nino ajeno.
 */
class CertificadoServiceTest {

    private UserService userService;
    private ContentService contentService;
    private SubmissionRepository submissions;
    private CertificadoRepository certificados;
    private ClassroomRepository salones;
    private ClassroomEnrollmentRepository inscripciones;
    private ParentStudentRepository familias;
    private CertificadoService servicio;

    private User victoria, papa, laura, pedro;
    private Subject materia;
    private Content p1, p2;

    private static User u(String username, UserRole rol) {
        return User.builder().id(UUID.randomUUID()).username(username).displayName(username + " Apellido")
                .role(rol).passwordHash("x").build();
    }

    private ContentResponse dto(Content c) {
        return ContentResponse.builder().id(c.getId()).subjectId(materia.getId())
                .subjectName(materia.getName()).estimatedMinutes((short) 20).build();
    }

    private Submission aprobada(Content c) {
        Submission s = new Submission();
        s.setContent(c); s.setStudent(victoria); s.setStatus(SubmissionStatus.aprobado);
        return s;
    }

    @BeforeEach
    void preparar() {
        userService = mock(UserService.class);
        contentService = mock(ContentService.class);
        submissions = mock(SubmissionRepository.class);
        certificados = mock(CertificadoRepository.class);
        salones = mock(ClassroomRepository.class);
        inscripciones = mock(ClassroomEnrollmentRepository.class);
        familias = mock(ParentStudentRepository.class);
        servicio = new CertificadoService(userService, contentService, mock(SubjectService.class), submissions,
                certificados, salones, inscripciones, familias, mock(UserRepository.class),
                mock(NotificationService.class));

        victoria = u("victoria", UserRole.student);
        papa = u("papa", UserRole.parent);
        laura = u("laura", UserRole.teacher);
        pedro = u("pedro", UserRole.teacher);
        for (User x : List.of(victoria, papa, laura, pedro)) when(userService.findByUsername(x.getUsername())).thenReturn(x);

        materia = new Subject();
        materia.setId(UUID.randomUUID());
        materia.setName("Mi Primera IA");
        p1 = new Content(); p1.setId(UUID.randomUUID());
        p2 = new Content(); p2.setId(UUID.randomUUID());
        when(contentService.findForStudent(victoria.getId())).thenReturn(List.of(dto(p1), dto(p2)));
        when(certificados.findByStudent(victoria)).thenReturn(List.of());

        Classroom salon = new Classroom();
        salon.setId(UUID.randomUUID()); salon.setTeacher(laura);
        when(salones.findByTeacherAndIsActiveTrue(laura)).thenReturn(List.of(salon));
        when(salones.findByTeacherAndIsActiveTrue(pedro)).thenReturn(List.of());
        when(inscripciones.findActiveStudentsByClassroom(salon)).thenReturn(List.of(victoria));
        when(familias.findChildrenByParent(papa)).thenReturn(List.of(victoria));
    }

    private Certificado certificado(boolean entregado) {
        Certificado c = Certificado.builder().id(UUID.randomUUID()).student(victoria).subject(materia)
                .folio("BK-2026-ABCDE").solicitadoEn(OffsetDateTime.now())
                .entregadoEn(entregado ? OffsetDateTime.now() : null).entregadoPor(entregado ? laura : null).build();
        when(certificados.findById(c.getId())).thenReturn(Optional.of(c));
        when(submissions.findByStudentOrderBySubmittedAtDesc(victoria)).thenReturn(List.of(aprobada(p1), aprobada(p2)));
        return c;
    }

    @Test
    void conUnaActividadSinAprobarNoSePuedePedir() {
        when(submissions.findByStudentOrderBySubmittedAtDesc(victoria)).thenReturn(List.of(aprobada(p1)));
        assertThat(servicio.misAvances("victoria").get(0).estado()).isEqualTo("en_curso");
        assertThatThrownBy(() -> servicio.solicitar("victoria", materia.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1 actividades");
        verify(certificados, never()).save(any());
    }

    @Test
    void conTodoAprobadoEstaListoParaPedir() {
        when(submissions.findByStudentOrderBySubmittedAtDesc(victoria)).thenReturn(List.of(aprobada(p1), aprobada(p2)));
        assertThat(servicio.misAvances("victoria").get(0).estado()).isEqualTo("listo");
    }

    @Test
    void elAlumnoYSuFamiliaNoLoVenAntesDeEntregarse() {
        Certificado c = certificado(false);
        assertThatThrownBy(() -> servicio.detalle("victoria", c.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.detalle("papa", c.getId())).isInstanceOf(AccessDeniedException.class);
        // Su maestra si, para revisarlo antes de entregarlo.
        assertThat(servicio.detalle("laura", c.getId()).valido()).isFalse();
    }

    @Test
    void entregadoLoVenElAlumnoYSuFamilia() {
        Certificado c = certificado(true);
        assertThat(servicio.detalle("victoria", c.getId()).folio()).isEqualTo("BK-2026-ABCDE");
        assertThat(servicio.detalle("papa", c.getId()).actividades()).isEqualTo(2);
    }

    @Test
    void otroMaestroNoLoVeNiLoEntrega() {
        Certificado c = certificado(false);
        assertThatThrownBy(() -> servicio.detalle("pedro", c.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.entregar("pedro", c.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(c.getEntregadoEn()).isNull();
    }
}
