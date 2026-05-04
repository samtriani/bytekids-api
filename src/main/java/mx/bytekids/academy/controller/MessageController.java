package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.message.MessageRequest;
import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.MessageService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Mensajes")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/inbox")
    @Operation(summary = "Bandeja de entrada")
    public ResponseEntity<ApiResponse<List<Message>>> inbox() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(messageService.findInbox(user.getId())));
    }

    @GetMapping("/sent")
    @Operation(summary = "Mensajes enviados")
    public ResponseEntity<ApiResponse<List<Message>>> sent() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(messageService.findSent(user.getId())));
    }

    @GetMapping("/thread/{parentId}")
    @Operation(summary = "Hilo de respuestas de un mensaje")
    public ResponseEntity<ApiResponse<List<Message>>> thread(@PathVariable UUID parentId) {
        return ResponseEntity.ok(ApiResponse.ok(messageService.findThread(parentId)));
    }

    @PostMapping
    @Operation(summary = "Enviar mensaje")
    public ResponseEntity<ApiResponse<Message>> send(@Valid @RequestBody MessageRequest req) {
        var sender = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Mensaje enviado", messageService.send(req, sender.getId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar mensaje como leído")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        messageService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Mensaje marcado como leído", null));
    }
}
