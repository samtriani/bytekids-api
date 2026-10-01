package mx.bytekids.academy.dto.familia;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un trabajo que entrego el hijo, tal como lo ve su familia: lo que escribio,
 * como va, su calificacion y lo que le dijo su maestro.
 *
 * estado: aprobada | revision | corregir
 * calificacion: 0-100, como se guarda (la pantalla la muestra sobre 10).
 * texto: null en los quizzes, que no tienen texto que leer.
 */
public record TrabajoResponse(
        UUID actividadId, String titulo, String tipo, String materia, String color, int orden,
        String estado, Integer calificacion, String texto, String comentario,
        String revisadoPor, OffsetDateTime entregadoEn, OffsetDateTime revisadoEn) {}
