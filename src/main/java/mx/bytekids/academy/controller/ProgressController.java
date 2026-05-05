package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.entity.DailyActivity;
import mx.bytekids.academy.entity.StudentSubjectProgress;
import mx.bytekids.academy.entity.XpEvent;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ProgressService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@Tag(name = "Progreso")
public class ProgressController {

    private final ProgressService progressService;
    private final UserService userService;

    @GetMapping("/me/xp")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "XP total del alumno autenticado")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> myXp() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("totalXp", progressService.getTotalXp(student.getId()))));
    }

    @GetMapping("/me/streak")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Racha actual del alumno autenticado")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> myStreak() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("streak", progressService.getCurrentStreak(student.getId()))));
    }

    @GetMapping("/me/subjects")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Progreso por materia del alumno autenticado")
    public ResponseEntity<ApiResponse<List<StudentSubjectProgress>>> mySubjectProgress() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(progressService.getSubjectProgress(student.getId())));
    }

    @GetMapping("/me/activity")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Actividad diaria (últimos 30 días)")
    public ResponseEntity<ApiResponse<List<DailyActivity>>> myActivity() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(progressService.getWeekActivity(student.getId())));
    }

    @GetMapping("/me/xp-history")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Historial de XP del alumno autenticado")
    public ResponseEntity<ApiResponse<List<XpEvent>>> myXpHistory() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(progressService.getXpHistory(student.getId())));
    }

    // ---- Endpoints para maestros / admin ----

    @GetMapping("/students/{studentId}/xp")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    @Operation(summary = "XP total de un alumno")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> studentXp(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("totalXp", progressService.getTotalXp(studentId))));
    }

    @GetMapping("/students/{studentId}/streak")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    @Operation(summary = "Racha actual de un alumno")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> studentStreak(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("streak", progressService.getCurrentStreak(studentId))));
    }

    @GetMapping("/students/{studentId}/subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    @Operation(summary = "Progreso por materia de un alumno")
    public ResponseEntity<ApiResponse<List<StudentSubjectProgress>>> studentSubjects(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(progressService.getSubjectProgress(studentId)));
    }

    @GetMapping("/students/{studentId}/activity")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    @Operation(summary = "Actividad diaria de un alumno")
    public ResponseEntity<ApiResponse<List<DailyActivity>>> studentActivity(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(progressService.getWeekActivity(studentId)));
    }

    @GetMapping("/leaderboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Top alumnos por XP")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(progressService.getLeaderboard(limit)));
    }
}
