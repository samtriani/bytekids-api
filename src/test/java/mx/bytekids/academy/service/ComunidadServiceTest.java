package mx.bytekids.academy.service;

import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.ClassroomEnrollment;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Las reglas de quien ve el muro de quien. Es lo unico de la Comunidad del
 * maestro que, si se rompe, expone a menores: un maestro no puede ver el
 * salon de otro ni felicitar a un nino que no es suyo.
 */
class ComunidadServiceTest {

    private UserService userService;
    private ClassroomRepository classroomRepository;
    private ClassroomEnrollmentRepository enrollmentRepository;
    private XpEventRepository xpEventRepository;
    private StudentAchievementRepository studentAchievementRepository;
    private NotificationRepository notificationRepository;
    private NotificationService notificationService;
    private ComunidadService servicio;

    private User laura, pedro, victoria, coordinadora;
    private Classroom salonDeLaura;

    private static User usuario(String username, String nombre, UserRole rol) {
        return User.builder().id(UUID.randomUUID()).username(username).displayName(nombre)
                .role(rol).passwordHash("x").build();
    }

    @BeforeEach
    void preparar() {
        userService = mock(UserService.class);
        classroomRepository = mock(ClassroomRepository.class);
        enrollmentRepository = mock(ClassroomEnrollmentRepository.class);
        xpEventRepository = mock(XpEventRepository.class);
        studentAchievementRepository = mock(StudentAchievementRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        notificationService = mock(NotificationService.class);
        servicio = new ComunidadService(userService, classroomRepository, enrollmentRepository,
                xpEventRepository, studentAchievementRepository, notificationRepository, notificationService);

        laura = usuario("laura", "Laura Maestra", UserRole.teacher);
        pedro = usuario("pedro", "Pedro Maestro", UserRole.teacher);
        victoria = usuario("victoria", "Victoria", UserRole.student);
        coordinadora = usuario("coord", "Coordinación", UserRole.admin);
        for (User u : List.of(laura, pedro, victoria, coordinadora)) {
            when(userService.findByUsername(u.getUsername())).thenReturn(u);
        }

        salonDeLaura = new Classroom();
        salonDeLaura.setId(UUID.randomUUID());
        salonDeLaura.setName("Mi Primera IA - Grupo 1");
        salonDeLaura.setTeacher(laura);
        salonDeLaura.setIsActive(true);
        when(classroomRepository.findById(salonDeLaura.getId())).thenReturn(Optional.of(salonDeLaura));
        when(enrollmentRepository.findActiveStudentsByClassroom(salonDeLaura)).thenReturn(List.of(victoria));

        ClassroomEnrollment inscripcion = new ClassroomEnrollment();
        inscripcion.setClassroom(salonDeLaura);
        inscripcion.setStudent(victoria);
        when(enrollmentRepository.findByStudentAndIsActiveTrue(victoria)).thenReturn(List.of(inscripcion));
    }

    private StudentAchievement logroDeVictoria() {
        AchievementDefinition def = new AchievementDefinition();
        def.setId(UUID.randomUUID());
        def.setTitle("Entrenador de IA");
        def.setIcon("🧠");
        StudentAchievement sa = new StudentAchievement();
        sa.setId(UUID.randomUUID());
        sa.setStudent(victoria);
        sa.setAchievement(def);
        when(studentAchievementRepository.findById(sa.getId())).thenReturn(Optional.of(sa));
        return sa;
    }

    @Test
    void laMaestraVeElMuroDeSuSalon() {
        var muro = servicio.muro("laura", salonDeLaura.getId());
        assertThat(muro.salon()).isEqualTo("Mi Primera IA - Grupo 1");
        // Sin XP, Victoria va a "sin actividad", no al ranking.
        assertThat(muro.sinActividad()).extracting("nombre").containsExactly("Victoria");
        assertThat(muro.ranking()).isEmpty();
    }

    @Test
    void otroMaestroNoVeElMuroAunqueTengaElId() {
        assertThatThrownBy(() -> servicio.muro("pedro", salonDeLaura.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void coordinacionVeCualquierSalon() {
        assertThat(servicio.muro("coord", salonDeLaura.getId()).totalAlumnos()).isEqualTo(1);
    }

    @Test
    void otroMaestroNoPuedeFelicitarAUnNinoQueNoEsSuyo() {
        StudentAchievement sa = logroDeVictoria();
        assertThatThrownBy(() -> servicio.felicitar("pedro", sa.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(notificationService);
    }

    @Test
    void laMaestraFelicitaUnaSolaVez() {
        StudentAchievement sa = logroDeVictoria();
        when(notificationRepository.referenciasEnviadas(eq(laura), eq("felicitacion"), anyCollection()))
                .thenReturn(List.of())                 // la primera vez no hay nada
                .thenReturn(List.of(sa.getId()));       // la segunda ya existe

        assertThat(servicio.felicitar("laura", sa.getId())).isTrue();
        assertThat(servicio.felicitar("laura", sa.getId())).isFalse();

        // Una sola notificacion, a Victoria, con el nombre de pila de la maestra.
        verify(notificationService, times(1)).avisar(eq(victoria), eq(laura), any(),
                eq("👏 ¡Laura te felicitó!"), contains("Entrenador de IA"), eq(sa.getId()), eq("felicitacion"));
    }
}
