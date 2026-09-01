package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.content.AssignContentRequest;
import mx.bytekids.academy.dto.content.ContentRequest;
import mx.bytekids.academy.dto.content.ContentResponse;
import mx.bytekids.academy.entity.enums.ContentType;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ContentService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
@Tag(name = "Contenido (Misiones / Proyectos / Quizzes)")
public class ContentController {

    private final ContentService contentService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Listar contenido publicado")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(contentService.findAll()));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Filtrar por tipo (mision, proyecto, quiz, tarea, material)")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> findByType(@PathVariable ContentType type) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.findByType(type)));
    }

    @GetMapping("/feed")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Contenido asignado al alumno autenticado (sus salones + asignaciones directas)")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> studentFeed() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(contentService.findForStudent(student.getId())));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Contenido creado por el maestro autenticado")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> myContent() {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(contentService.findByTeacher(teacher.getId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener contenido por ID")
    public ResponseEntity<ApiResponse<ContentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ContentResponse.from(contentService.findById(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Crear contenido")
    public ResponseEntity<ApiResponse<ContentResponse>> create(@Valid @RequestBody ContentRequest req) {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Contenido creado", contentService.create(req, teacher.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Actualizar contenido")
    public ResponseEntity<ApiResponse<ContentResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody ContentRequest req) {
        var actor = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(contentService.update(id, req, actor.getId())));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Publicar contenido")
    public ResponseEntity<ApiResponse<ContentResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Contenido publicado", contentService.publish(id)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Asignar a salón o alumno individual")
    public ResponseEntity<ApiResponse<Void>> assign(@PathVariable UUID id,
                                                     @RequestBody AssignContentRequest req) {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        contentService.assign(id, req, teacher.getId());
        return ResponseEntity.ok(ApiResponse.ok("Contenido asignado", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Desactivar contenido")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        var actor = userService.findByUsername(SecurityUtils.currentUsername());
        contentService.deactivate(id, actor.getId());
        return ResponseEntity.ok(ApiResponse.ok("Contenido desactivado", null));
    }
}
