package mx.bytekids.academy.dto.comunidad;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * El muro de un salon, visto por su maestro.
 *
 * Es el mismo muro que ve el alumno --logros recientes y como va el salon--
 * mas una cosa que el alumno NO debe ver: quien todavia no tiene actividad.
 * En la pantalla del nino eso seria exhibir a un companero; en la del
 * maestro es la forma de no dejarlo atras.
 *
 * De cada alumno sale lo mismo que en un mensaje: nombre, iniciales y robot.
 * Nada de correo, edad ni direccion.
 */
public record ComunidadSalonResponse(
        UUID salonId,
        String salon,
        int totalAlumnos,
        List<Alumno> ranking,
        List<Alumno> sinActividad,
        List<Logro> logros) {

    public record Alumno(UUID id, String nombre, String iniciales, String avatarUrl, int xp, int puesto) {}

    public record Logro(UUID id, UUID alumnoId, String nombre, String iniciales, String avatarUrl,
                        String titulo, String icono, int xp, OffsetDateTime cuando,
                        boolean felicitado) {}

    public record Salon(UUID id, String nombre, int alumnos) {}
}
