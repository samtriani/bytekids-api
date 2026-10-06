package mx.bytekids.academy.repository;

import mx.bytekids.academy.entity.Certificado;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {

    List<Certificado> findByStudent(User student);

    Optional<Certificado> findByStudentAndSubject(User student, Subject subject);

    boolean existsByFolio(String folio);

    java.util.Optional<Certificado> findByFolio(String folio);

    /** Los de estos alumnos, pendientes primero y luego los entregados mas recientes. */
    @Query("SELECT c FROM Certificado c JOIN FETCH c.student JOIN FETCH c.subject " +
           "WHERE c.student.id IN :alumnos ORDER BY c.entregadoEn DESC NULLS FIRST, c.solicitadoEn ASC")
    List<Certificado> deAlumnos(Collection<UUID> alumnos);

    @Query("SELECT c FROM Certificado c JOIN FETCH c.student JOIN FETCH c.subject " +
           "ORDER BY c.entregadoEn DESC NULLS FIRST, c.solicitadoEn ASC")
    List<Certificado> todos();
}
