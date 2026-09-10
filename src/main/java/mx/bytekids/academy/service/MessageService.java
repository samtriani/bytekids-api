package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.message.ContactoResponse;
import mx.bytekids.academy.dto.message.MessageRequest;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.ClassroomEnrollment;
import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.entity.ParentStudent;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.ClassroomEnrollmentRepository;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.MessageRepository;
import mx.bytekids.academy.repository.ParentStudentRepository;
import mx.bytekids.academy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public List<Message> findInbox(UUID userId) {
        User user = userService.findById(userId);
        return messageRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public List<Message> findSent(UUID userId) {
        User user = userService.findById(userId);
        return messageRepository.findBySenderOrderByCreatedAtDesc(user);
    }

    /**
     * El hilo de un mensaje, solo si quien pregunta es parte de la conversación.
     *
     * Antes no se verificaba nada: con el UUID de un mensaje cualquiera podía
     * leer la conversación completa de otras dos personas. En una plataforma
     * de menores eso no se deja abierto ni aunque los UUID no se adivinen.
     */
    public List<Message> findThread(UUID parentId, UUID solicitanteId) {
        Message parent = messageRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", parentId));

        List<Message> hilo = new ArrayList<>();
        hilo.add(parent);
        hilo.addAll(messageRepository.findByParentMessageOrderByCreatedAtAsc(parent));

        boolean participa = hilo.stream().anyMatch(m ->
                m.getSender().getId().equals(solicitanteId)
             || m.getRecipient().getId().equals(solicitanteId));
        if (!participa) {
            throw new BusinessException("Esta conversación no es tuya");
        }
        return hilo;
    }

    @Transactional
    public Message send(MessageRequest req, UUID senderId) {
        User sender = userService.findById(senderId);
        User recipient = userService.findById(req.getRecipientId());

        if (sender.getId().equals(recipient.getId())) {
            throw new BusinessException("No puedes escribirte a ti mismo");
        }
        // La MISMA lista que alimenta el selector valida el envío. Si se
        // calcularan por separado, tarde o temprano una dejaría pasar algo que
        // la otra no muestra --y el agujero estaría del lado que no se ve--.
        //
        // La segunda condición es para no dejar conversaciones mudas: a quien
        // ya te escribió siempre le puedes contestar, aunque el vínculo se
        // haya deshecho. Sin esto, un alumno que cambia de salón deja al
        // maestro con un hilo abierto que no puede responder, y el permiso no
        // lo estaría dando la pantalla sino el otro, que fue quien tocó.
        boolean permitido = contactosPermitidos(sender).containsKey(recipient.getId())
                         || messageRepository.existsBySenderAndRecipient(recipient, sender);
        if (!permitido) {
            throw new BusinessException("No puedes enviarle mensajes a esta persona");
        }

        Message parent = req.getParentMessageId() != null
                ? messageRepository.findById(req.getParentMessageId()).orElse(null) : null;

        Message message = Message.builder()
                .sender(sender).recipient(recipient)
                .subject(req.getSubject()).body(req.getBody())
                .parentMessage(parent)
                .build();
        return messageRepository.save(message);
    }

    @Transactional
    public void markAsRead(UUID messageId, UUID recipientId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", messageId));
        if (message.getRecipient().getId().equals(recipientId)) {
            message.setIsRead(true);
            message.setReadAt(OffsetDateTime.now());
            messageRepository.save(message);
        }
    }

    /** A quién le puede escribir este usuario, para pintarle el selector. */
    public List<ContactoResponse> contactos(UUID userId) {
        return new ArrayList<>(contactosPermitidos(userService.findById(userId)).values());
    }

    // ── Quién le puede escribir a quién ─────────────────────────────────────

    /**
     * La regla de la casa, en un solo lugar.
     *
     *   alumno  → los maestros de sus salones, y coordinación.
     *   maestro → sus alumnos, los familiares de sus alumnos, y coordinación.
     *   familiar→ los maestros de sus hijos, y coordinación.
     *   staff   → cualquiera.
     *
     * Lo que NO aparece por ningún lado es alumno → alumno. Un chat libre
     * entre menores es una superficie que esta plataforma no quiere abrir, y
     * la forma de no abrirla es que la regla no exista, no que la pantalla no
     * la ofrezca.
     */
    private Map<UUID, ContactoResponse> contactosPermitidos(User yo) {
        Map<UUID, ContactoResponse> permitidos = new LinkedHashMap<>();

        switch (yo.getRole()) {
            case student -> {
                for (ClassroomEnrollment ins : enrollmentRepository.findByStudentAndIsActiveTrue(yo)) {
                    Classroom salon = ins.getClassroom();
                    agregar(permitidos, salon.getTeacher(), "Tu maestro de " + salon.getName());
                }
                agregarPersonal(permitidos);
            }
            case teacher -> {
                for (Classroom salon : classroomRepository.findByTeacherAndIsActiveTrue(yo)) {
                    for (User alumno : enrollmentRepository.findActiveStudentsByClassroom(salon)) {
                        agregar(permitidos, alumno, "Tu alumno en " + salon.getName());
                        for (ParentStudent lazo : parentStudentRepository.findByStudent(alumno)) {
                            agregar(permitidos, lazo.getParent(),
                                    "Familiar de " + alumno.getDisplayName());
                        }
                    }
                }
                agregarPersonal(permitidos);
            }
            case parent -> {
                for (User hijo : parentStudentRepository.findChildrenByParent(yo)) {
                    for (ClassroomEnrollment ins : enrollmentRepository.findByStudentAndIsActiveTrue(hijo)) {
                        Classroom salon = ins.getClassroom();
                        agregar(permitidos, salon.getTeacher(),
                                "Maestro de " + hijo.getDisplayName() + " en " + salon.getName());
                    }
                }
                agregarPersonal(permitidos);
            }
            case admin, director -> {
                for (UserRole rol : List.of(UserRole.director, UserRole.admin,
                                            UserRole.teacher, UserRole.parent, UserRole.student)) {
                    for (User u : userRepository.findByRoleAndIsActiveTrue(rol)) {
                        agregar(permitidos, u, etiquetaDeRol(rol));
                    }
                }
            }
        }

        permitidos.remove(yo.getId());
        return permitidos;
    }

    /** Coordinación y dirección: siempre alcanzables, para cualquiera. */
    private void agregarPersonal(Map<UUID, ContactoResponse> destino) {
        for (UserRole rol : List.of(UserRole.director, UserRole.admin)) {
            for (User u : userRepository.findByRoleAndIsActiveTrue(rol)) {
                agregar(destino, u, etiquetaDeRol(rol));
            }
        }
    }

    private static String etiquetaDeRol(UserRole rol) {
        return switch (rol) {
            case director -> "Dirección";
            case admin    -> "Coordinación";
            case teacher  -> "Maestro";
            case parent   -> "Familiar";
            case student  -> "Alumno";
        };
    }

    /**
     * El primer motivo gana: un maestro que además es tu tutor aparece una
     * vez, como maestro, que es el vínculo por el que le vas a escribir.
     */
    private static void agregar(Map<UUID, ContactoResponse> destino, User u, String motivo) {
        if (u == null || !Boolean.TRUE.equals(u.getIsActive())) return;
        destino.computeIfAbsent(u.getId(), id -> ContactoResponse.builder()
                .id(id)
                .displayName(u.getDisplayName())
                .initials(u.getInitials())
                .role(u.getRole())
                .avatarUrl(u.getAvatarUrl())
                .motivo(motivo)
                .build());
    }
}
