package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.entity.AiConversation;
import mx.bytekids.academy.entity.AiMessage;
import mx.bytekids.academy.entity.enums.AiContextType;
import mx.bytekids.academy.entity.enums.MessageRole;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.AiConversationService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai/conversations")
@RequiredArgsConstructor
@Tag(name = "Tutor IA — Conversaciones")
public class AiConversationController {

    private final AiConversationService aiService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mis conversaciones con ByteBot")
    public ResponseEntity<ApiResponse<List<AiConversation>>> myConversations() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(aiService.findByStudent(student.getId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Iniciar nueva conversación con ByteBot")
    public ResponseEntity<ApiResponse<AiConversation>> start(
            @RequestParam(defaultValue = "general") AiContextType contextType,
            @RequestParam(required = false) UUID contextId) {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Conversación iniciada",
                        aiService.startConversation(student.getId(), contextType, contextId)));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mensajes de una conversación")
    public ResponseEntity<ApiResponse<List<AiMessage>>> messages(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.findMessages(id)));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Agregar mensaje a conversación (user o assistant)")
    public ResponseEntity<ApiResponse<AiMessage>> addMessage(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        MessageRole role = MessageRole.valueOf(body.getOrDefault("role", "user"));
        String content = body.get("content");
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(aiService.addMessage(id, role, content)));
    }
}
