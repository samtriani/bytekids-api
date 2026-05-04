package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.classroom.ClassroomRequest;
import mx.bytekids.academy.dto.classroom.ClassroomResponse;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ClassroomService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/classrooms")
@RequiredArgsConstructor
@Tag(name = "Salones")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Listar todos los salones activos")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Obtener salon por ID")
    public ResponseEntity<ApiResponse<ClassroomResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ClassroomResponse.from(classroomService.findById(id))));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Salones de un profesor")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> findByTeacher(@PathVariable UUID teacherId) {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.findByTeacher(teacherId)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Mis salones")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> myClassrooms() {
        var teacher = userService.findByUsername(SecurityUtils.currentUsername());
        return ResponseEntity.ok(ApiResponse.ok(classroomService.findByTeacher(teacher.getId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear salon")
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(@Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Salon creado", classroomService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar salon")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.update(id, request)));
    }

    @PostMapping("/{id}/enroll/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inscribir alumno a salon")
    public ResponseEntity<ApiResponse<Void>> enroll(@PathVariable UUID id, @PathVariable UUID studentId) {
        classroomService.enroll(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Alumno inscrito", null));
    }

    @DeleteMapping("/{id}/enroll/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja alumno de salon")
    public ResponseEntity<ApiResponse<Void>> unenroll(@PathVariable UUID id, @PathVariable UUID studentId) {
        classroomService.unenroll(id, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Alumno dado de baja", null));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Alumnos inscritos en un salon")
    public ResponseEntity<ApiResponse<List<UserResponse>>> students(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.findStudents(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Salones en los que está inscrito un alumno")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> classroomsByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.findByStudent(studentId)));
    }
}
