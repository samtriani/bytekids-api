package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.entity.QuizAttempt;
import mx.bytekids.academy.entity.QuizOption;
import mx.bytekids.academy.entity.QuizQuestion;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.QuizService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
@Tag(name = "Quizzes")
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;

    @GetMapping("/{contentId}/questions")
    @Operation(summary = "Preguntas de un quiz")
    public ResponseEntity<ApiResponse<List<QuizQuestion>>> questions(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.findQuestions(contentId)));
    }

    @PostMapping("/{contentId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Agregar pregunta a un quiz")
    public ResponseEntity<ApiResponse<QuizQuestion>> addQuestion(@PathVariable UUID contentId,
                                                                  @RequestBody QuizQuestion question) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Pregunta agregada", quizService.addQuestion(contentId, question)));
    }

    @PostMapping("/questions/{questionId}/options")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Agregar opción a una pregunta")
    public ResponseEntity<ApiResponse<QuizOption>> addOption(@PathVariable UUID questionId,
                                                              @RequestBody QuizOption option) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Opción agregada", quizService.addOption(questionId, option)));
    }

    // Body: { "answers": { "<questionId>": "<optionId>" }, "assignmentId": "<uuid>" }
    @PostMapping("/{contentId}/attempt")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Responder quiz — devuelve calificación inmediata")
    public ResponseEntity<ApiResponse<QuizAttempt>> submit(
            @PathVariable UUID contentId,
            @RequestBody Map<String, Object> body) {
        var student = userService.findByUsername(SecurityUtils.currentUsername());

        @SuppressWarnings("unchecked")
        Map<String, String> rawAnswers = (Map<String, String>) body.get("answers");
        Map<UUID, UUID> answers = rawAnswers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> UUID.fromString(e.getKey()),
                        e -> e.getValue() != null ? UUID.fromString(e.getValue()) : null
                ));

        String rawAssignmentId = (String) body.get("assignmentId");
        UUID assignmentId = rawAssignmentId != null ? UUID.fromString(rawAssignmentId) : null;

        QuizAttempt attempt = quizService.submitAttempt(contentId, student.getId(), answers, assignmentId);
        return ResponseEntity.status(201).body(ApiResponse.ok("Quiz completado", attempt));
    }
}
