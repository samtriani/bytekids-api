package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.submission.ReviewRequest;
import mx.bytekids.academy.dto.submission.SubmissionRequest;
import mx.bytekids.academy.dto.submission.SubmissionResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.SubmissionService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
@Tag(name = "Entregas")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Entregar misión / tarea / proyecto")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(@Valid @RequestBody SubmissionRequest req) {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Entrega registrada", submissionService.submit(req, student.getId())));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mis entregas (alumno autenticado)")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> mySubmissions() {
        var student = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(submissionService.findByStudent(student.getId())));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Entregas de un alumno específico")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> byStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.findByStudent(studentId)));
    }

    @GetMapping("/content/{contentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Entregas para un contenido específico")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> byContent(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.findByContent(contentId)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Entregas pendientes de revisión")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.findPending()));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "Calificar entrega (aprobado / rechazado)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> review(@PathVariable UUID id,
                                                                   @Valid @RequestBody ReviewRequest req) {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok("Entrega calificada",
                submissionService.review(id, req, teacher.getId())));
    }
}
