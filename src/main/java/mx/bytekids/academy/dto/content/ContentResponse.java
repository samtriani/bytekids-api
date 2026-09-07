package mx.bytekids.academy.dto.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.security.SecurityUtils;
import lombok.Data;
import mx.bytekids.academy.entity.Content;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.entity.enums.DifficultyLevel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class ContentResponse {
    private UUID id;
    private String title;
    private String description;
    private ContentType type;
    private UUID subjectId;
    private String subjectName;
    private String subjectIcon;
    private String subjectColor;
    private UUID createdById;
    private String createdByName;
    private Short xpReward;
    private DifficultyLevel difficulty;
    private Short estimatedMinutes;
    private String contentBody;
    private Short orderIndex;
    private Boolean isPublished;
    private OffsetDateTime createdAt;
    private OffsetDateTime dueDate;

    /**
     * true cuando lo creo coordinacion o direccion: es plan base de la escuela y
     * el maestro no lo edita. Se deriva del rol del autor, sin columna nueva.
     */
    private Boolean basePlan;

    public static ContentResponse from(Content c) {
        return ContentResponse.builder()
                .id(c.getId()).title(c.getTitle()).description(c.getDescription())
                .type(c.getType())
                .subjectId(c.getSubject() != null ? c.getSubject().getId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                .subjectIcon(c.getSubject() != null ? c.getSubject().getIcon() : null)
                .subjectColor(c.getSubject() != null ? c.getSubject().getColor() : null)
                .createdById(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getDisplayName())
                .xpReward(c.getXpReward()).difficulty(c.getDifficulty())
                .estimatedMinutes(c.getEstimatedMinutes()).contentBody(cuerpoVisible(c.getContentBody()))
                .orderIndex(c.getOrderIndex()).isPublished(c.getIsPublished())
                .createdAt(c.getCreatedAt())
                .dueDate(c.getDueDate())
                .basePlan(esPlanBase(c))
                .build();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Llaves de content_body que son para el personal de la escuela, no para el
     * alumno: la respuesta esperada, el criterio de correccion y la guia de clase.
     */
    private static final List<String> SOLO_PERSONAL =
            List.of("expected_output", "solution_check", "teacher_notes");

    /**
     * El alumno recibe el content_body sin las llaves de arriba.
     *
     * Se filtra aqui, en el unico punto por el que sale todo el contenido, y no
     * en cada endpoint: el DTO es compartido por el feed del alumno y por las
     * vistas del maestro, y ya se filtro dos veces por olvidarlo (la respuesta
     * esperada y el isCorrect de las opciones de quiz llegaban al navegador del
     * alumno aunque la pantalla no los pintara).
     */
    private static String cuerpoVisible(String body) {
        if (body == null || body.isBlank()) return body;

        boolean personal = SecurityUtils.hasRole("TEACHER")
                        || SecurityUtils.hasRole("ADMIN")
                        || SecurityUtils.hasRole("DIRECTOR");
        if (personal) return body;

        try {
            JsonNode raiz = MAPPER.readTree(body);
            if (!raiz.isObject()) return body;
            ObjectNode obj = (ObjectNode) raiz;
            boolean quitoAlgo = false;
            for (String llave : SOLO_PERSONAL) {
                if (obj.remove(llave) != null) quitoAlgo = true;
            }
            return quitoAlgo ? MAPPER.writeValueAsString(obj) : body;
        } catch (Exception e) {
            // content_body viejo en texto plano: no hay llaves que quitar.
            return body;
        }
    }

    private static boolean esPlanBase(Content c) {
        var rol = c.getCreatedBy().getRole();
        return rol == UserRole.admin || rol == UserRole.director;
    }
}
