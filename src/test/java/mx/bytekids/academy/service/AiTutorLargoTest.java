package mx.bytekids.academy.service;

import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El largo de una pregunta a ByteBot: 1000 para alumnos, 4000 para los
 * demas. Pasarse no se corta en silencio: ByteBot pide que pregunte por partes.
 */
class AiTutorLargoTest {

    private static User de(UserRole rol) { return User.builder().role(rol).build(); }

    @Test
    void unAlumnoTieneMilLetras() {
        User alumno = de(UserRole.student);
        assertThat(AiTutorService.siEsMuyLarga(alumno, "a".repeat(1000))).isNull();
        assertThat(AiTutorService.siEsMuyLarga(alumno, "a".repeat(1001)))
                .contains("por partes").contains("1 000");
    }

    @Test
    void maestrosYFamiliasTienenMasEspacio() {
        assertThat(AiTutorService.siEsMuyLarga(de(UserRole.teacher), "a".repeat(3000))).isNull();
        assertThat(AiTutorService.siEsMuyLarga(de(UserRole.parent), "a".repeat(4000))).isNull();
        assertThat(AiTutorService.siEsMuyLarga(de(UserRole.teacher), "a".repeat(4001))).isNotNull();
    }
}
