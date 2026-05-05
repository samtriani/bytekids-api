package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.admin.ClassroomAssignmentRequest;
import mx.bytekids.academy.dto.admin.ClassroomTeacherAssignmentRequest;
import mx.bytekids.academy.dto.classroom.ClassroomRequest;
import mx.bytekids.academy.dto.classroom.ClassroomResponse;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.Subject;
import mx.bytekids.academy.service.AdministratorService;
import mx.bytekids.academy.service.ClassroomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/administrator")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administrador")
public class AdministratorController {

    private final AdministratorService administratorService;
    private final ClassroomService classroomService;

    @PostMapping("/users")
    @Operation(summary = "Dar de alta usuarios")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Usuario creado", administratorService.createUser(request)));
    }

    @PostMapping("/classrooms")
    @Operation(summary = "Dar de alta salones")
    public ResponseEntity<ApiResponse<ClassroomResponse>> createClassroom(@Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Salon creado", administratorService.createClassroom(request)));
    }

    @PostMapping("/subjects")
    @Operation(summary = "Dar de alta materias")
    public ResponseEntity<ApiResponse<Subject>> createSubject(@RequestBody Subject subject) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Materia creada", administratorService.createSubject(subject)));
    }

    @PutMapping("/classrooms/teacher")
    @Operation(summary = "Asignar profesor a salon")
    public ResponseEntity<ApiResponse<ClassroomResponse>> assignTeacher(
            @Valid @RequestBody ClassroomTeacherAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profesor asignado",
                administratorService.assignTeacherToClassroom(request)));
    }

    @DeleteMapping("/classrooms/{classroomId}/teacher")
    @Operation(summary = "Quitar profesor de salon")
    public ResponseEntity<ApiResponse<ClassroomResponse>> unassignTeacher(@PathVariable UUID classroomId) {
        return ResponseEntity.ok(ApiResponse.ok("Profesor removido",
                administratorService.unassignTeacherFromClassroom(classroomId)));
    }

    @PostMapping("/classrooms/student")
    @Operation(summary = "Inscribir alumno a salon")
    public ResponseEntity<ApiResponse<Void>> assignStudent(
            @Valid @RequestBody ClassroomAssignmentRequest request) {
        administratorService.assignStudentToClassroom(request);
        return ResponseEntity.ok(ApiResponse.ok("Alumno inscrito", null));
    }

    @DeleteMapping("/classrooms/{classroomId}/students/{studentId}")
    @Operation(summary = "Dar de baja alumno de salon")
    public ResponseEntity<ApiResponse<Void>> unassignStudent(
            @PathVariable UUID classroomId, @PathVariable UUID studentId) {
        administratorService.unassignStudentFromClassroom(classroomId, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Alumno removido", null));
    }

    @DeleteMapping("/classrooms/{classroomId}")
    @Operation(summary = "Dar de baja lógica a un salón — queda inactivo y deja de aparecer en listas")
    public ResponseEntity<ApiResponse<Void>> deactivateClassroom(@PathVariable UUID classroomId) {
        administratorService.deactivateClassroom(classroomId);
        return ResponseEntity.ok(ApiResponse.ok("Salón dado de baja", null));
    }

    @PostMapping("/classrooms/{classroomId}/subjects/{subjectId}")
    @Operation(summary = "Asignar materia a salón")
    public ResponseEntity<ApiResponse<Void>> addSubject(
            @PathVariable UUID classroomId, @PathVariable UUID subjectId) {
        classroomService.addSubject(classroomId, subjectId);
        return ResponseEntity.ok(ApiResponse.ok("Materia asignada", null));
    }

    @DeleteMapping("/classrooms/{classroomId}/subjects/{subjectId}")
    @Operation(summary = "Quitar materia de salón")
    public ResponseEntity<ApiResponse<Void>> removeSubject(
            @PathVariable UUID classroomId, @PathVariable UUID subjectId) {
        classroomService.removeSubject(classroomId, subjectId);
        return ResponseEntity.ok(ApiResponse.ok("Materia removida", null));
    }
}
