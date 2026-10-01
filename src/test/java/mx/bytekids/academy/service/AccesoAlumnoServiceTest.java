package mx.bytekids.academy.service;

import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.ParentStudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Quien puede ver los datos de un nino. Antes /progress/students/{id} y
 * /achievements/students/{id} solo revisaban el rol: cualquier papa veia a
 * cualquier nino conociendo su id.
 */
class AccesoAlumnoServiceTest {

    private ParentStudentRepository familias;
    private ClassroomRepository salones;
    private ClassroomEnrollmentRepository inscripciones;
    private AccesoAlumnoService acceso;

    private User victoria, otroNino, papaDeVictoria, otroPapa, laura, pedro, coordinacion;

    private static User u(UserRole rol) {
        return User.builder().id(UUID.randomUUID()).username(UUID.randomUUID().toString()).role(rol).build();
    }

    @BeforeEach
    void preparar() {
        familias = mock(ParentStudentRepository.class);
        salones = mock(ClassroomRepository.class);
        inscripciones = mock(ClassroomEnrollmentRepository.class);
        acceso = new AccesoAlumnoService(mock(UserService.class), familias, salones, inscripciones);

        victoria = u(UserRole.student); otroNino = u(UserRole.student);
        papaDeVictoria = u(UserRole.parent); otroPapa = u(UserRole.parent);
        laura = u(UserRole.teacher); pedro = u(UserRole.teacher);
        coordinacion = u(UserRole.admin);

        when(familias.findChildrenByParent(papaDeVictoria)).thenReturn(List.of(victoria));
        when(familias.findChildrenByParent(otroPapa)).thenReturn(List.of(otroNino));

        Classroom salon = new Classroom();
        salon.setId(UUID.randomUUID()); salon.setTeacher(laura);
        when(salones.findByTeacherAndIsActiveTrue(laura)).thenReturn(List.of(salon));
        when(salones.findByTeacherAndIsActiveTrue(pedro)).thenReturn(List.of());
        when(inscripciones.findActiveStudentsByClassroom(salon)).thenReturn(List.of(victoria));
    }

    @Test
    void unPapaVeASuHijoYNoAlDeOtro() {
        assertThat(acceso.puedeVer(papaDeVictoria, victoria.getId())).isTrue();
        assertThat(acceso.puedeVer(otroPapa, victoria.getId())).isFalse();
    }

    @Test
    void unMaestroVeASusAlumnosYNoALosDeOtroSalon() {
        assertThat(acceso.puedeVer(laura, victoria.getId())).isTrue();
        assertThat(acceso.puedeVer(pedro, victoria.getId())).isFalse();
        assertThat(acceso.puedeVer(laura, otroNino.getId())).isFalse();
    }

    @Test
    void unAlumnoSoloSeVeASiMismo() {
        assertThat(acceso.puedeVer(victoria, victoria.getId())).isTrue();
        assertThat(acceso.puedeVer(victoria, otroNino.getId())).isFalse();
    }

    @Test
    void coordinacionVeATodos() {
        assertThat(acceso.puedeVer(coordinacion, otroNino.getId())).isTrue();
    }
}
