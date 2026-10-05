package mx.bytekids.academy.service;

import mx.bytekids.academy.service.SeguimientoService.Diagnostico;
import mx.bytekids.academy.service.SeguimientoService.Hechos;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las reglas de quien necesita atencion. La que importa mas es la primera:
 * ir a la mitad del curso NO es necesitar apoyo.
 */
class SeguimientoServiceTest {

    private static final OffsetDateTime HOY = OffsetDateTime.parse("2026-10-05T12:00:00-06:00");

    private static OffsetDateTime haceDias(int d) { return HOY.minusDays(d); }

    /** Un nino al corriente: 3 de 9, activo ayer, sin pendientes. */
    private static Hechos alCorriente() {
        return new Hechos(haceDias(10), 3, 9, haceDias(1), null, null, null, 0, 90, 2);
    }

    private static Diagnostico de(Hechos h) { return SeguimientoService.diagnosticar(h, HOY); }

    @Test
    void irAlTercioDelCursoNoEsNecesitarApoyo() {
        assertThat(de(alCorriente()).estado()).isEqualTo("bien");
    }

    @Test
    void sinNadaHechoEsNuevoLosPrimerosDiasYDespuesSinEmpezar() {
        assertThat(de(new Hechos(haceDias(1), 0, 9, null, null, null, null, 0, null, 0)).estado()).isEqualTo("nuevo");
        Diagnostico d = de(new Hechos(haceDias(4), 0, 9, null, null, null, null, 0, null, 0));
        assertThat(d.estado()).isEqualTo("sin_empezar");
        assertThat(d.razon()).contains("hace 4 días");
    }

    @Test
    void unaCorreccionSinReenviarTresDiasEsAtorado() {
        Hechos h = new Hechos(haceDias(10), 3, 9, haceDias(1), "Misión 2", haceDias(3), null, 0, 90, 2);
        Diagnostico d = de(h);
        assertThat(d.estado()).isEqualTo("atorado");
        assertThat(d.razon()).contains("Misión 2", "hace 3 días");
        // Recien pedida no es alarma: le toca al nino corregir.
        assertThat(de(new Hechos(haceDias(10), 3, 9, haceDias(1), "Misión 2", haceDias(1), null, 0, 90, 2)).estado())
                .isEqualTo("bien");
    }

    @Test
    void reprobarElMismoQuizDosVecesEsAtorado() {
        Diagnostico d = de(new Hechos(haceDias(10), 3, 9, haceDias(1), null, null, "Quiz: ¿IA o no IA?", 2, 90, 2));
        assertThat(d.estado()).isEqualTo("atorado");
        assertThat(d.razon()).contains("¿IA o no IA?", "2 veces");
    }

    @Test
    void cincoDiasSinHacerNadaEsSinActividadSalvoQueYaTermino() {
        assertThat(de(new Hechos(haceDias(20), 3, 9, haceDias(6), null, null, null, 0, 90, 2)).razon())
                .contains("6 días");
        assertThat(de(new Hechos(haceDias(20), 9, 9, haceDias(6), null, null, null, 0, 90, 2)).estado())
                .isEqualTo("bien");
    }

    @Test
    void promedioBajoSoloConDosOMasCalificadas() {
        Diagnostico d = de(new Hechos(haceDias(10), 3, 9, haceDias(1), null, null, null, 0, 55, 2));
        assertThat(d.estado()).isEqualTo("calificaciones_bajas");
        assertThat(d.razon()).contains("5.5");
        // Una sola calificacion baja no es tendencia.
        assertThat(de(new Hechos(haceDias(10), 3, 9, haceDias(1), null, null, null, 0, 55, 1)).estado())
                .isEqualTo("bien");
    }
}
