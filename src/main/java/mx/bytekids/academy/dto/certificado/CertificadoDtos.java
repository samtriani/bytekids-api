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

    /**
     * Todo lo que se imprime. proyectos: misiones y proyectos aprobados, lo
     * que el nino construyo (los materiales y quizzes cuentan en actividades).
     * logros: los que ha desbloqueado en ByteKids. Va en el certificado en vez
     * de las horas: en un curso corto, "3.5 horas" se leia como poco.
     */
    public record Detalle(UUID id, String folio, String alumno, String avatarUrl, String iniciales,
                          String materia, String color, int actividades, int minutos, int proyectos, int logros,
                          OffsetDateTime solicitadoEn, OffsetDateTime entregadoEn,
                          String entregadoPor, boolean valido) {}

    /**
     * Lo que ve cualquiera que escanee el QR, sin iniciar sesion. Es la pagina
     * publica de un menor: solo el nombre con la inicial del apellido
     * ("Maria Z."), el curso y la fecha. Quien verifica ya tiene el
     * certificado en la mano; esto solo confirma que es real.
     */
    public record Verificacion(String folio, String alumno, String materia, OffsetDateTime entregadoEn) {}
}
