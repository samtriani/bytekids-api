package mx.bytekids.academy.dto.submission;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.enums.SubmissionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class SubmissionResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentInitials;
    private UUID contentId;
    private String contentTitle;
    private String codeSubmitted;
    private SubmissionStatus status;
    private Short score;
    private Short attemptsCount;
    private String teacherFeedback;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;

    /**
     * Largo REAL de codeSubmitted, aunque el texto venga recortado.
     *
     * Sin esto el frontend no puede distinguir "el alumno escribio 4000
     * caracteres" de "escribio 900 mil y te mandamos los primeros 4000", y le
     * diria al maestro que no falta nada cuando si falta.
     */
    private Integer codeLength;

    /**
     * Cuanto texto de la entrega viaja en los listados del maestro.
     *
     * La Libreta pide TODAS las entregas de un alumno para abrir una sola, asi
     * que una entrega enorme se descargaba completa cada vez que el maestro
     * abria cualquier celda de esa fila. Con un nino recargado en una tecla
     * eso es una respuesta de megabytes y la pantalla se queda en
     * "Cargando la entrega...".
     *
     * El alumno SI recibe su texto completo: su pantalla lo vuelve a cargar en
     * el recuadro editable para reenviar despues de "Pedir correcciones", y
     * recortarlo ahi le borraria su propio trabajo.
     */
    private static final int RESUMEN_MAX = 4000;

    /** Version ligera para los listados del maestro. */
    public static SubmissionResponse resumen(Submission s) {
        SubmissionResponse r = from(s);
        String texto = s.getCodeSubmitted();
        if (texto != null && texto.length() > RESUMEN_MAX) {
            r.setCodeSubmitted(texto.substring(0, RESUMEN_MAX));
        }
        return r;
    }

    public static SubmissionResponse from(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getDisplayName())
                .studentInitials(s.getStudent().getInitials())
                .contentId(s.getContent().getId())
                .contentTitle(s.getContent().getTitle())
                .codeSubmitted(s.getCodeSubmitted())
                .status(s.getStatus())
                .score(s.getScore())
                .attemptsCount(s.getAttemptsCount())
                .teacherFeedback(s.getTeacherFeedback())
                .submittedAt(s.getSubmittedAt())
                .reviewedAt(s.getReviewedAt())
                .codeLength(s.getCodeSubmitted() != null ? s.getCodeSubmitted().length() : 0)
                .build();
    }
}
