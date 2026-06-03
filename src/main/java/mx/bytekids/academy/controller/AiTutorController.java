package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.ai.AiChatRequest;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.AiTutorService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-tutor")
@RequiredArgsConstructor
@Tag(name = "Tutor IA")
public class AiTutorController {

    private final AiTutorService aiTutorService;
    private final UserService userService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Chat con ByteBot — tutor personalizado por usuario")
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody AiChatRequest request) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        var reply = aiTutorService.chat(user, request.getHistory(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(reply));
    }
}
