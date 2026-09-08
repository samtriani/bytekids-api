package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.content.AssignContentRequest;
import mx.bytekids.academy.dto.content.ContentRequest;
import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.entity.*;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.ContentAssignmentRepository;
import mx.bytekids.academy.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository           contentRepository;
    private final ContentAssignmentRepository  assignmentRepository;
    private final SubjectService               subjectService;
    private final UserService                  userService;
    private final ClassroomService             classroomService;
    private final ClassroomRepository          classroomRepository;

    public Content findById(UUID id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", id));
    }

    public List<ContentResponse> findAll() {
        return contentRepository.findByIsPublishedTrueAndIsActiveTrueOrderBySubjectAscOrderIndexAsc()
                .stream().map(ContentResponse::from).toList();
    }

    public List<ContentResponse> findByType(ContentType type) {
        return contentRepository.findByTypeAndIsActiveTrue(type)
                .stream().map(ContentResponse::from).toList();
    }

    /**
     * Todo el contenido con el que trabaja un maestro: lo que el creó (editable)
     * mas lo asignado a sus salones, que incluye el plan base de coordinacion
     * (solo lectura).
     *
     * Antes filtraba unicamente por autoria, asi que al adoptar el plan base la
     * pantalla del maestro se quedaba vacia aunque siguiera impartiendo esa
     * clase todos los dias.
     */
    public List<ContentResponse> findByTeacher(UUID teacherId) {
        User teacher = userService.findById(teacherId);
        Set<UUID> vistos = new HashSet<>();
        List<Content> resultado = new java.util.ArrayList<>();

        contentRepository.findByCreatedByAndIsActiveTrue(teacher).stream()
                .filter(c -> vistos.add(c.getId()))
                .forEach(resultado::add);

        classroomRepository.findByTeacherAndIsActiveTrue(teacher).forEach(salon ->
                assignmentRepository.findByClassroomAndIsActiveTrue(salon).stream()
                        .map(ContentAssignment::getContent)
                        .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                        .filter(c -> vistos.add(c.getId()))
                        .forEach(resultado::add));

        resultado.sort(java.util.Comparator
                .comparing((Content c) -> c.getSubject() != null ? c.getSubject().getName() : "")
                .thenComparing(c -> c.getOrderIndex() != null ? c.getOrderIndex().intValue() : Integer.MAX_VALUE)
                .thenComparing(Content::getTitle));

        return resultado.stream().map(ContentResponse::from).toList();
    }

    @Transactional
    public ContentResponse create(ContentRequest req, UUID creatorId) {
        User creator = userService.findById(creatorId);
        Subject subject = req.getSubjectId() != null ? subjectService.findById(req.getSubjectId()) : null;

        Content content = Content.builder()
                .title(req.getTitle()).description(req.getDescription())
                .type(req.getType()).subject(subject).createdBy(creator)
                .xpReward(req.getXpReward() != null ? req.getXpReward() : 50)
                .difficulty(req.getDifficulty() != null ? req.getDifficulty() : mx.bytekids.academy.entity.enums.DifficultyLevel.medio)
                .estimatedMinutes(req.getEstimatedMinutes())
                .contentBody(req.getContentBody())
                .orderIndex(req.getOrderIndex())
                .dueDate(req.getDueDate())
                .isPublished(true)   // auto-publicar al crear
                .build();

        Content saved = contentRepository.save(content);

        // Se asigna a los salones que pide la peticion, no a "todos los del creador".
        // Aquel comportamiento (a) ignoraba el salon elegido en el formulario,
        // (b) duplicaba la asignacion porque la UI llama assign() despues, y
        // (c) no servia para coordinacion, que no es titular de ningun salon.
        if (req.getClassroomIds() != null) {
            req.getClassroomIds().stream().distinct().forEach(classroomId ->
                assignmentRepository.save(ContentAssignment.builder()
                        .content(saved)
                        .classroom(classroomService.findById(classroomId))
                        .assignedBy(creator).build()));
        }

        return ContentResponse.from(saved);
    }

    /**
     * Modelo hibrido: coordinacion y direccion son dueñas del plan base, y el
     * maestro complementa con contenido propio para su grupo. Por eso un maestro
     * no puede tocar lo que creo coordinacion, aunque se lo hayan asignado.
     * Coordinacion si puede editar cualquier cosa.
     */
    private void exigirPropiedad(Content content, UUID actorId) {
        User actor = userService.findById(actorId);
        boolean esCoordinacion = actor.getRole() == UserRole.admin || actor.getRole() == UserRole.director;
        if (esCoordinacion) return;

        boolean esSuyo = content.getCreatedBy().getId().equals(actorId);
        if (!esSuyo) {
            throw new BusinessException(
                    "Este contenido es del plan base de la escuela. Pide a coordinación que lo ajuste, "
                  + "o crea uno propio para tu grupo.");
        }
    }

    @Transactional
    public ContentResponse update(UUID id, ContentRequest req, UUID actorId) {
        Content content = findById(id);
        exigirPropiedad(content, actorId);
        content.setTitle(req.getTitle());
        content.setDescription(req.getDescription());
        if (req.getSubjectId() != null) content.setSubject(subjectService.findById(req.getSubjectId()));
        if (req.getXpReward() != null) content.setXpReward(req.getXpReward());
        if (req.getDifficulty() != null) content.setDifficulty(req.getDifficulty());
        content.setEstimatedMinutes(req.getEstimatedMinutes());
        content.setContentBody(req.getContentBody());
        content.setOrderIndex(req.getOrderIndex());
        content.setDueDate(req.getDueDate());
        return ContentResponse.from(contentRepository.save(content));
    }

    /**
     * Coordinacion adopta un contenido al plan base: pasa a ser su dueña y el
     * maestro deja de poder editarlo. Sirve para promover algo que hizo un
     * maestro y merece ser institucional, y para corregir la autoria de lo que
     * se cargo con la cuenta equivocada.
     */
    @Transactional
    public ContentResponse adoptar(UUID contentId, UUID nuevoDuenoId) {
        User nuevoDueno = userService.findById(nuevoDuenoId);
        if (nuevoDueno.getRole() != UserRole.admin && nuevoDueno.getRole() != UserRole.director) {
            throw new BusinessException("Solo coordinación o dirección pueden ser dueños del plan base.");
        }
        Content content = findById(contentId);
        content.setCreatedBy(nuevoDueno);
        return ContentResponse.from(contentRepository.save(content));
    }

    @Transactional
    public ContentResponse publish(UUID id) {
        Content content = findById(id);
        content.setIsPublished(true);
        return ContentResponse.from(contentRepository.save(content));
    }

    @Transactional
    public ContentAssignment assign(UUID contentId, AssignContentRequest req, UUID assignedById) {
        if (req.getClassroomId() == null && req.getStudentId() == null) {
            throw new BusinessException("Debe especificar un salón o un alumno");
        }
        if (req.getClassroomId() != null && req.getStudentId() != null) {
            throw new BusinessException("No puede asignar a salón y alumno al mismo tiempo");
        }

        Content content = findById(contentId);
        User assignedBy = userService.findById(assignedById);
        Classroom classroom = req.getClassroomId() != null ? classroomService.findById(req.getClassroomId()) : null;
        User student = req.getStudentId() != null ? userService.findById(req.getStudentId()) : null;

        // Asignar dos veces al mismo salon no debe crear dos filas: el alumno
        // veria la pieza duplicada en su lista de misiones.
        if (classroom != null) {
            var previa = assignmentRepository
                    .findByContentAndClassroomAndIsActiveTrue(content, classroom);
            if (!previa.isEmpty()) return previa.get(0);
        }
        if (student != null) {
            var previa = assignmentRepository
                    .findByContentAndStudentAndIsActiveTrue(content, student);
            if (!previa.isEmpty()) return previa.get(0);
        }

        ContentAssignment assignment = ContentAssignment.builder()
                .content(content).classroom(classroom).student(student)
                .assignedBy(assignedBy).dueDate(req.getDueDate())
                .build();
        return assignmentRepository.save(assignment);
    }

    /** El contenido que hoy recibe un salon, en el orden del curriculo. */
    public List<ContentResponse> findForClassroom(UUID classroomId) {
        Classroom salon = classroomService.findById(classroomId);
        return assignmentRepository.findByClassroomAndIsActiveTrue(salon).stream()
                .map(ContentAssignment::getContent)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .sorted(java.util.Comparator
                        .comparing((Content c) -> c.getSubject() != null
                                ? c.getSubject().getName() : "")
                        .thenComparing(c -> c.getOrderIndex() != null
                                ? c.getOrderIndex() : Short.MAX_VALUE))
                .map(ContentResponse::from)
                .toList();
    }

    /** Baja logica: las entregas que ya hicieron los alumnos se conservan. */
    @Transactional
    public void desasignarDeSalon(UUID contentId, UUID classroomId) {
        Content content = findById(contentId);
        Classroom salon = classroomService.findById(classroomId);
        var asignaciones = assignmentRepository
                .findByContentAndClassroomAndIsActiveTrue(content, salon);
        if (asignaciones.isEmpty()) {
            throw new BusinessException("Esa pieza no esta asignada a este salon");
        }
        asignaciones.forEach(a -> a.setIsActive(false));
        assignmentRepository.saveAll(asignaciones);
    }

    /**
     * Asigna de un golpe todo el plan base publicado de una materia.
     * Es el caso real: montar un salon nuevo son 12 o 17 piezas, y hacerlo
     * de una en una no lo hace nadie.
     */
    @Transactional
    public int asignarMateriaASalon(UUID subjectId, UUID classroomId, UUID actorId) {
        Subject materia = subjectService.findById(subjectId);
        Classroom salon = classroomService.findById(classroomId);
        User actor     = userService.findById(actorId);

        int nuevas = 0;
        for (Content c : contentRepository.findBySubjectAndIsActiveTrue(materia)) {
            if (!Boolean.TRUE.equals(c.getIsPublished())) continue;
            if (!assignmentRepository
                    .findByContentAndClassroomAndIsActiveTrue(c, salon).isEmpty()) continue;
            assignmentRepository.save(ContentAssignment.builder()
                    .content(c).classroom(salon).assignedBy(actor).build());
            nuevas++;
        }
        return nuevas;
    }

    /**
     * Lo que el alumno recibe A TITULO PERSONAL, sin pasar por un salon.
     * Es la membresia de solo contenido: da acceso al temario y al tutor de
     * IA, pero no a las clases en vivo, que cuelgan del horario de un salon.
     *
     * A proposito NO incluye lo que le llega por estar inscrito en un salon:
     * esta lista es la que coordinacion administra a mano, y mezclar las dos
     * llevaria a intentar quitar de aqui algo que en realidad viene del salon.
     */
    public List<ContentResponse> findAssignedDirectlyTo(UUID studentId) {
        User student = userService.findById(studentId);
        return assignmentRepository.findByStudentAndIsActiveTrue(student).stream()
                .map(ContentAssignment::getContent)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .sorted(java.util.Comparator
                        .comparing((Content c) -> c.getSubject() != null
                                ? c.getSubject().getName() : "")
                        .thenComparing(c -> c.getOrderIndex() != null
                                ? c.getOrderIndex() : Short.MAX_VALUE))
                .map(ContentResponse::from)
                .toList();
    }

    /** Baja logica: al vencer la membresia se conserva lo que ya entrego. */
    @Transactional
    public void desasignarDeAlumno(UUID contentId, UUID studentId) {
        Content content = findById(contentId);
        User student    = userService.findById(studentId);
        var asignaciones = assignmentRepository
                .findByContentAndStudentAndIsActiveTrue(content, student);
        if (asignaciones.isEmpty()) {
            throw new BusinessException("Esa pieza no esta asignada a este alumno");
        }
        asignaciones.forEach(a -> a.setIsActive(false));
        assignmentRepository.saveAll(asignaciones);
    }

    /** Alta de la membresia: todo el plan base publicado de una materia. */
    @Transactional
    public int asignarMateriaAAlumno(UUID subjectId, UUID studentId, UUID actorId) {
        Subject materia = subjectService.findById(subjectId);
        User student    = userService.findById(studentId);
        User actor      = userService.findById(actorId);

        if (student.getRole() != UserRole.student) {
            throw new BusinessException("Solo se le puede asignar contenido a un alumno");
        }

        int nuevas = 0;
        for (Content c : contentRepository.findBySubjectAndIsActiveTrue(materia)) {
            if (!Boolean.TRUE.equals(c.getIsPublished())) continue;
            if (!assignmentRepository
                    .findByContentAndStudentAndIsActiveTrue(c, student).isEmpty()) continue;
            assignmentRepository.save(ContentAssignment.builder()
                    .content(c).student(student).assignedBy(actor).build());
            nuevas++;
        }
        return nuevas;
    }

    /** Baja completa de la membresia individual de una materia. */
    @Transactional
    public int quitarMateriaAAlumno(UUID subjectId, UUID studentId) {
        Subject materia = subjectService.findById(subjectId);
        User student    = userService.findById(studentId);

        var bajas = assignmentRepository.findByStudentAndIsActiveTrue(student).stream()
                .filter(a -> a.getContent().getSubject() != null
                          && a.getContent().getSubject().getId().equals(materia.getId()))
                .peek(a -> a.setIsActive(false))
                .toList();
        assignmentRepository.saveAll(bajas);
        return bajas.size();
    }

    public List<ContentResponse> findForStudent(UUID studentId) {
        User student = userService.findById(studentId);
        Set<UUID> seen = new HashSet<>();
        return assignmentRepository.findAllAssignmentsForStudent(student)
                .stream()
                .map(ContentAssignment::getContent)
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()) && Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> seen.add(c.getId()))
                .map(ContentResponse::from)
                .toList();
    }

    @Transactional
    public void deactivate(UUID id, UUID actorId) {
        Content content = findById(id);
        exigirPropiedad(content, actorId);
        content.setIsActive(false);
        contentRepository.save(content);
    }
}
