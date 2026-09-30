package mx.bytekids.academy.dto.certificado;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Lo que viaja de los certificados. De cada alumno: nombre y robot, nada mas. */
public final class CertificadoDtos {

    private CertificadoDtos() {}

    /**
     * Como va el alumno hacia el certificado de una materia.
     * estado: en_curso | listo | solicitado | entregado
     */
    public record Avance(UUID subjectId, String materia, String color, String icono,
                         int aprobadas, int total, String estado,
                         UUID certificadoId, String folio, OffsetDateTime entregadoEn) {}

    /** Un certificado en la lista del maestro. */
    public record Fila(UUID id, String folio, UUID alumnoId, String alumno, String avatarUrl,
                       String iniciales, String materia, OffsetDateTime solicitadoEn,
                       OffsetDateTime entregadoEn) {}

    /** Todo lo que se imprime. */
    public record Detalle(UUID id, String folio, String alumno, String avatarUrl, String iniciales,
                          String materia, String color, int actividades, int minutos,
                          OffsetDateTime solicitadoEn, OffsetDateTime entregadoEn,
                          String entregadoPor, boolean valido) {}
}
