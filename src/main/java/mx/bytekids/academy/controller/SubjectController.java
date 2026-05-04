package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Tag(name = "Materias")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Listar materias activas")
    public ResponseEntity<ApiResponse<List<Subject>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Obtener materia por ID")
    public ResponseEntity<ApiResponse<Subject>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear materia")
    public ResponseEntity<ApiResponse<Subject>> create(@RequestBody Subject subject) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Materia creada", subjectService.create(subject)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar materia")
    public ResponseEntity<ApiResponse<Subject>> update(@PathVariable UUID id, @RequestBody Subject subject) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.update(id, subject)));
    }
}
