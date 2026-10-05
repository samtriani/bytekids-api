package mx.bytekids.academy.dto.seguimiento;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Como va cada alumno de un salon, para el panel del maestro.
 *
 * El avance y la alerta van separados: un nino con 3 de 9 actividades en su
 * segundo dia va perfecto, y antes salia en rojo con "Apoyo" solo por el
 * porcentaje. La alerta (estado + razon) sale solo cuando hay algo concreto
 * que hacer. Las reglas viven en SeguimientoService.diagnosticar.
 */
public record SeguimientoResponse(UUID salonId, int piezas, List<Alumno> alumnos) {

    /**
     * estado: bien | nuevo | sin_empezar | atorado | sin_actividad | calificaciones_bajas
     * hechas: entregadas (aunque no esten calificadas) + materiales vistos.
     * promedio: 0-100, de lo ya calificado; null si no hay nada calificado.
     * razon: por que tiene ese estado, en una frase para el maestro.
     */
    public record Alumno(
            UUID id, String nombre, String iniciales, String avatarUrl,
            int hechas, int aprobadas, int porCalificar, Integer promedio,
            OffsetDateTime ultimaActividad, String estado, String razon) {}
}
