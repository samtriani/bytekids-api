package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.message.ContactoResponse;
import mx.bytekids.academy.dto.message.MessageRequest;
import mx.bytekids.academy.entity.Message;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.MessageService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Mensajes")
// Este controlador no tenia ni un @PreAuthorize. No se notaba porque el
// alumno no tenia pantalla desde donde llamarlo, pero la mensajeria es de
// los cinco roles y quien decide a quien se le puede escribir es
// MessageService.contactosPermitidos(), no la pantalla.
@PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER','STUDENT','PARENT')")
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
        // El servicio verifica que quien pregunta sea parte del hilo.
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.findThread(parentId, user.getId())));
    }

    @GetMapping("/contactos")
    @Operation(summary = "A quien le puedo escribir")
    public ResponseEntity<ApiResponse<List<ContactoResponse>>> contactos() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(messageService.contactos(user.getId())));
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
