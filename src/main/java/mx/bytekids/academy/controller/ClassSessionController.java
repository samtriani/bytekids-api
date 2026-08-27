package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ClassSessionService;
import mx.bytekids.academy.service.JaasTokenService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Sesiones de clase")
public class ClassSessionController {

    private final ClassSessionService sessionService;
    private final JaasTokenService    jaasTokenService;
    private final UserService         userService;

    @GetMapping("/schedule/{scheduleId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER','STUDENT')")
    @Operation(summary = "Estado actual de un horario (activo o no)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getStatus(scheduleId)));
    }

    @PostMapping("/schedule/{scheduleId}/join")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @Operation(summary = "Entrar al aula virtual")
    public ResponseEntity<ApiResponse<Void>> join(@PathVariable UUID scheduleId) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        sessionService.join(scheduleId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Ingresaste al aula", null));
    }

    @PostMapping("/schedule/{scheduleId}/leave")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @Operation(summary = "Salir del aula virtual")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable UUID scheduleId) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        sessionService.leave(scheduleId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Saliste del aula", null));
    }

    @GetMapping("/schedule/{scheduleId}/attendance")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN','DIRECTOR')")
    @Operation(summary = "Participantes activos + estado de video del maestro")
    public ResponseEntity<ApiResponse<Map<String, Object>>> attendance(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getAttendance(scheduleId)));
    }

    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR')")
    @Operation(summary = "Clases con videollamada activa en este momento (supervisión)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> live() {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getLiveSessions()));
    }

    /**
     * ADMIN y DIRECTOR obtienen el token pero NO llaman a join(): entran como
     * observadores y no se registran en la asistencia de la clase.
     */
    @GetMapping("/schedule/{scheduleId}/jaas-token")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN','DIRECTOR')")
    @Operation(summary = "JWT firmado para JaaS (Jitsi as a Service)")
    public ResponseEntity<ApiResponse<String>> jaasToken(@PathVariable UUID scheduleId) {
        var user  = userService.findByUsername(SecurityUtils.currentUsername());
        // JaaS JWT: 'room' claim = short room name only (NO appId prefix)
        var room  = "ByteKids-" + scheduleId.toString().replace("-", "");
        var token = jaasTokenService.generateToken(user, room);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    @PostMapping("/schedule/{scheduleId}/video")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Maestro activa/desactiva videollamada")
    public ResponseEntity<ApiResponse<Boolean>> toggleVideo(@PathVariable UUID scheduleId) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        boolean active = sessionService.toggleVideo(scheduleId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(active ? "Videollamada iniciada" : "Videollamada finalizada", active));
    }

    @PostMapping("/schedule/{scheduleId}/mission")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Maestro lanza la misión del día")
    public ResponseEntity<ApiResponse<Map<String, Object>>> launchMission(
            @PathVariable UUID scheduleId,
            @RequestParam UUID contentId) {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok("Misión lanzada",
                sessionService.launchMission(scheduleId, contentId, teacher.getId())));
    }

    @PostMapping("/schedule/{scheduleId}/chat")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @Operation(summary = "Enviar mensaje al chat del aula")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendChat(
            @PathVariable UUID scheduleId,
            @RequestParam String content) {
        var user = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(sessionService.sendChatMessage(scheduleId, user.getId(), content)));
    }

    @GetMapping("/schedule/{scheduleId}/chat")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN','DIRECTOR')")
    @Operation(summary = "Mensajes del chat del aula de hoy")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChat(
            @PathVariable UUID scheduleId,
            @RequestParam(required = false) String since) {
        OffsetDateTime sinceTime = since != null && !since.isBlank()
                ? OffsetDateTime.parse(since) : null;
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getChatMessages(scheduleId, sinceTime)));
    }

    @GetMapping("/schedule/{scheduleId}/mission")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN','DIRECTOR')")
    @Operation(summary = "Obtener misión activa de la sesión")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMission(@PathVariable UUID scheduleId) {
        Optional<Map<String, Object>> mission = sessionService.getCurrentMission(scheduleId);
        return mission
                .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null)));
    }
}
