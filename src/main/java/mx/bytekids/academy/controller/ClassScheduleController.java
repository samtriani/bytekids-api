package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.schedule.ClassScheduleRequest;
import mx.bytekids.academy.dto.schedule.ClassScheduleResponse;
import mx.bytekids.academy.service.ClassScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "Horarios de clase")
public class ClassScheduleController {

    private final ClassScheduleService scheduleService;

    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Horario de un salón")
    public ResponseEntity<ApiResponse<List<ClassScheduleResponse>>> byClassroom(
            @PathVariable UUID classroomId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findByClassroom(classroomId)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Horario de un maestro")
    public ResponseEntity<ApiResponse<List<ClassScheduleResponse>>> byTeacher(
            @PathVariable UUID teacherId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findByTeacher(teacherId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear entrada de horario (valida conflictos de maestro y salón)")
    public ResponseEntity<ApiResponse<ClassScheduleResponse>> create(
            @Valid @RequestBody ClassScheduleRequest req) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Horario creado", scheduleService.create(req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar entrada de horario")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        scheduleService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Horario eliminado", null));
    }
}
