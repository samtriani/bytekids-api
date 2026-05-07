package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ClassSessionService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Sesiones de clase")
public class ClassSessionController {

    private final ClassSessionService sessionService;
    private final UserService userService;

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
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN')")
    @Operation(summary = "Participantes activos en la sesión de hoy")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> attendance(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getAttendance(scheduleId)));
    }
}
