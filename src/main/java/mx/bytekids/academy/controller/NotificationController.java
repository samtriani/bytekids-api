package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.notification.NotificationRequest;
import mx.bytekids.academy.entity.Notification;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.NotificationService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Notificaciones del usuario autenticado")
    public ResponseEntity<ApiResponse<List<Notification>>> myNotifications() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(notificationService.findByRecipient(user.getId())));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Cantidad de notificaciones no leídas")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unread", notificationService.countUnread(user.getId()))));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar notificación como leída")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída", null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Marcar todas las notificaciones como leídas")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Todas marcadas como leídas", null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Enviar notificación a un usuario")
    public ResponseEntity<ApiResponse<Notification>> send(@Valid @RequestBody NotificationRequest req) {
        var sender = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Notificación enviada", notificationService.send(req, sender.getId())));
    }
}
