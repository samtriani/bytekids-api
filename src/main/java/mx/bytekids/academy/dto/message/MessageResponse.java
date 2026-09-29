package mx.bytekids.academy.dto.message;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un mensaje, tal como lo ve quien lo lee.
 *
 * Antes se devolvia la entidad Message, y con ella el User completo de las
 * DOS personas: hash de contrasena, correo, edad y direccion. Un mensaje es
 * justo lo que cruza datos entre gente distinta --un alumno y su maestra, una
 * maestra y un papa--, asi que cada lado recibia el expediente del otro.
 *
 * De cada persona sale solo lo que una conversacion necesita pintar: nombre,
 * rol, iniciales y robot. Tampoco el username: es media credencial de
 * acceso, y la pantalla ya se arregla con el nombre.
 */
@Data
@Builder
public class MessageResponse {

    private UUID id;
    private Persona sender;
    private Persona recipient;
    private String subject;
    private String body;
    private Boolean isRead;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
    /** Solo el id: el mensaje padre entero traeria otra vez a las personas. */
    private UUID parentMessageId;

    @Data
    @Builder
    public static class Persona {
        private UUID id;
        private String displayName;
        private String role;
        private String initials;
        private String avatarUrl;

        static Persona de(User u) {
            if (u == null) return null;
            return Persona.builder()
                    .id(u.getId())
                    .displayName(u.getDisplayName())
                    .role(u.getRole() != null ? u.getRole().name() : null)
                    .initials(u.getInitials())
                    .avatarUrl(u.getAvatarUrl())
                    .build();
        }
    }

    public static MessageResponse from(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .sender(Persona.de(m.getSender()))
                .recipient(Persona.de(m.getRecipient()))
                .subject(m.getSubject())
                .body(m.getBody())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .readAt(m.getReadAt())
                .parentMessageId(m.getParentMessage() != null ? m.getParentMessage().getId() : null)
                .build();
    }
}
