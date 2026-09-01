package mx.bytekids.academy.dto.quiz;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.QuizOption;
import mx.bytekids.academy.entity.QuizQuestion;
import mx.bytekids.academy.entity.enums.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Pregunta con sus opciones, tal como la ve quien contesta.
 *
 * NUNCA incluye isCorrect: el alumno puede abrir las herramientas del
 * navegador y leer la respuesta de la red. La calificacion la resuelve
 * el backend en submitAttempt(), que si consulta cual era la correcta.
 *
 * Tampoco arrastra el Content completo: la entidad cruda serializaba la
 * materia entera y hasta el hibernateLazyInitializer.
 */
@Data
@Builder
public class QuizQuestionResponse {

    private UUID         id;
    private String       questionText;
    private QuestionType questionType;
    private Short        points;
    private Short        orderIndex;
    private List<OptionResponse> options;

    @Data
    @Builder
    public static class OptionResponse {
        private UUID   id;
        private String optionText;
        private Short  orderIndex;
    }

    public static QuizQuestionResponse from(QuizQuestion q, List<QuizOption> options) {
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType())
                .points(q.getPoints())
                .orderIndex(q.getOrderIndex())
                .options(options.stream()
                        .map(o -> OptionResponse.builder()
                                .id(o.getId())
                                .optionText(o.getOptionText())
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .toList())
                .build();
    }
}
