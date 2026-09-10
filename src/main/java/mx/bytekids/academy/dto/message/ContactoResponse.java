package mx.bytekids.academy.dto.message;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.enums.UserRole;

import java.util.UUID;

/**
 * Alguien a quien el usuario SI le puede escribir.
 *
 * El motivo no es adorno: es lo que le permite a un niño de nueve años saber
 * a cuál de tres personas con bata le está escribiendo. "Tu maestro de IA
 * para Niños (Intermedio)" dice más que un nombre suelto.
 */
@Data
@Builder
public class ContactoResponse {
    private UUID id;
    private String displayName;
    private String initials;
    private UserRole role;
    private String avatarUrl;

    /** Por qué aparece en la lista: "Tu maestro de ...", "Coordinación". */
    private String motivo;
}
