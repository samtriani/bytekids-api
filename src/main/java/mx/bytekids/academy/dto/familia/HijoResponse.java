package mx.bytekids.academy.dto.familia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Todo lo que un papa necesita de un hijo, en una sola respuesta.
 *
 * Sale de /familia/hijos, que NO recibe ids: devuelve los hijos de quien
 * pregunta y de nadie mas. Del nino viaja lo que su familia ya sabe --nombre,
 * robot, avance--; nada de credenciales.
 */
public record HijoResponse(
        UUID id, String nombre, String iniciales, String avatarUrl,
        int xp, int racha, OffsetDateTime ultimaActividad,
        List<Materia> materias, List<Logro> logros, List<Certificado> certificados,
        List<Clase> clases, List<XpDia> xpReciente) {

    public record Materia(UUID id, String nombre, String color, String icono,
                          int aprobadas, int total, Paso siguiente, List<Paso> camino) {}

    /** estado: aprobada | revision | corregir | siguiente | abierta | bloqueada */
    public record Paso(UUID id, int orden, String titulo, String tipo, String estado) {}

    public record Logro(String titulo, String icono, int xp, OffsetDateTime cuando) {}

    /** estado: solicitado | entregado */
    public record Certificado(UUID id, String materia, String estado, String folio) {}

    public record Clase(String dia, LocalTime inicio, LocalTime fin, String salon, String materia) {}

    public record XpDia(LocalDate fecha, int xp) {}
}
