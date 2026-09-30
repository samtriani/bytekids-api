package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.MissionPrerequisite;
import mx.bytekids.academy.entity.Submission;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.SubmissionStatus;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.repository.MissionPrerequisiteRepository;
import mx.bytekids.academy.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Que actividades tiene desbloqueadas un alumno.
 *
 * Usa la tabla mission_prerequisites, que existia desde el principio y nadie
 * consultaba. Una actividad sin prerrequisitos esta abierta, asi que los
 * cursos que no los cargan --los de paga, las misiones que el maestro lanza
 * en clase-- se comportan igual que antes. Mi Primera IA los trae: cada pieza
 * pide la anterior.
 *
 * CUANDO CUENTA UN PRERREQUISITO COMO HECHO
 *   - Un quiz: cuando esta APROBADO. Se califica solo y se puede repetir,
 *     asi que no detiene a nadie, y el siguiente paso da por sabido lo que
 *     el quiz comprobo.
 *   - Todo lo demas: cuando esta ENTREGADO, aunque el maestro no lo haya
 *     revisado o le haya pedido correcciones. Amarrarlo a la aprobacion
 *     dejaria a un nino que entrega el viernes en la noche atorado el fin de
 *     semana, y un nino atorado en un curso gratis se va. La aprobacion del
 *     maestro sigue mandando en lo que la necesita: logros y certificado.
 *
 * Una sola regla para la lista, la pantalla y las entregas: si cada una la
 * calculara por su lado, terminarian en desacuerdo, y el agujero estaria en
 * la que no se ve.
 */
@Service
@RequiredArgsConstructor
public class DesbloqueoService {

    private final MissionPrerequisiteRepository prerequisiteRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * De estas actividades, las bloqueadas, cada una con el primer
     * prerrequisito que le falta (el de menor order_index: lo siguiente que
     * el nino tiene que hacer).
     */
    @Transactional(readOnly = true)
    public Map<UUID, Content> bloqueadas(User alumno, Collection<Content> actividades) {
        if (actividades.isEmpty()) return Map.of();

        List<MissionPrerequisite> requisitos = prerequisiteRepository.conRequisitos(actividades);
        if (requisitos.isEmpty()) return Map.of();

        Set<UUID> hechas = hechasPor(alumno);
        Map<UUID, Content> faltan = new HashMap<>();
        for (MissionPrerequisite mp : requisitos) {
            Content req = mp.getPrerequisite();
            if (hechas.contains(req.getId())) continue;
            faltan.merge(mp.getMission().getId(), req, (a, b) -> orden(b) < orden(a) ? b : a);
        }
        return faltan;
    }

    /** Si esta actividad esta bloqueada, lo que le falta; si no, vacio. */
    @Transactional(readOnly = true)
    public Optional<Content> loQueFalta(User alumno, Content actividad) {
        return Optional.ofNullable(bloqueadas(alumno, List.of(actividad)).get(actividad.getId()));
    }

    /**
     * Para las entregas: el backend no confia en que la pantalla haya
     * escondido el boton. Entrar directo por la URL no salta el orden.
     */
    public void exigirDesbloqueada(User alumno, Content actividad) {
        loQueFalta(alumno, actividad).ifPresent(falta -> {
            throw new BusinessException("Primero termina «" + falta.getTitle() + "»");
        });
    }

    private Set<UUID> hechasPor(User alumno) {
        Set<UUID> hechas = new HashSet<>();
        for (Submission s : submissionRepository.findByStudentOrderBySubmittedAtDesc(alumno)) {
            Content c = s.getContent();
            if (c == null) continue;
            boolean cuenta = c.getType() == ContentType.quiz
                    ? s.getStatus() == SubmissionStatus.aprobado
                    : s.getStatus() != null;
            if (cuenta) hechas.add(c.getId());
        }
        return hechas;
    }

    private static int orden(Content c) {
        return c.getOrderIndex() == null ? Integer.MAX_VALUE : c.getOrderIndex();
    }
}
