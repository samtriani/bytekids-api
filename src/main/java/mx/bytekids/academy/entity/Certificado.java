package mx.bytekids.academy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El certificado de una materia terminada.
 *
 * Nace cuando el alumno lo SOLICITA y se vuelve valido cuando un maestro o
 * coordinacion lo ENTREGA. No sale solo al terminar, a proposito: la entrega
 * es el momento de hablar con la familia --una llamada de graduacion, la
 * invitacion al curso completo--, y un PDF que aparece solo se descarga y se
 * olvida.
 *
 * Uno por alumno y materia. Tabla creada en sql/2026-09-30_bytebot_limite_y_certificados.sql.
 */
@Entity
@Table(name = "certificados",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /** Lo que se imprime y sirve para verificarlo: BK-2026-7KQ4M. */
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @Column(name = "solicitado_en", nullable = false)
    private OffsetDateTime solicitadoEn;

    /** Null mientras nadie lo entrega: ahi todavia no es valido. */
    @Column(name = "entregado_en")
    private OffsetDateTime entregadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregado_por")
    private User entregadoPor;
}
