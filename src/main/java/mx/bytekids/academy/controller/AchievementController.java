package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.entity.AchievementDefinition;
import mx.bytekids.academy.entity.StudentAchievement;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.AchievementService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
@Tag(name = "Logros")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Catálogo completo de logros")
    public ResponseEntity<ApiResponse<List<AchievementDefinition>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(achievementService.findAllDefinitions()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener definición de logro por ID")
    public ResponseEntity<ApiResponse<AchievementDefinition>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(achievementService.findDefinitionById(id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Logros ganados por el alumno autenticado")
    public ResponseEntity<ApiResponse<List<StudentAchievement>>> myAchievements() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(achievementService.findEarnedByStudent(student.getId())));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    @Operation(summary = "Logros de un alumno")
    public ResponseEntity<ApiResponse<List<StudentAchievement>>> studentAchievements(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(achievementService.findEarnedByStudent(studentId)));
    }

    @PostMapping("/award/{studentId}/{achievementId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Otorgar logro manualmente a un alumno")
    public ResponseEntity<ApiResponse<StudentAchievement>> award(@PathVariable UUID studentId,
                                                                  @PathVariable UUID achievementId) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Logro otorgado", achievementService.award(studentId, achievementId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear definición de logro")
    public ResponseEntity<ApiResponse<AchievementDefinition>> create(@RequestBody AchievementDefinition def) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Logro creado", achievementService.createDefinition(def)));
    }
}
