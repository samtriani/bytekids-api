package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.user.CambioContrasenaRequest;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.ParentStudent;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.repository.ParentStudentRepository;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class UserController {

    private final UserService userService;
    private final ParentStudentRepository parentStudentRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR')")
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll()));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        String username = SecurityUtils.currentUsername();
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(userService.findByUsername(username))));
    }

    /**
     * Cambiar la contrasena propia. Sin rol: la usan los cinco.
     *
     * No lleva id en la ruta a proposito. El unico PUT que escribia
     * contrasenas era /users/{id}, restringido a ADMIN, y eso dejaba a
     * maestros, familias y alumnos sin ninguna forma de cambiar la suya
     * --incluida la que se les mando por mensaje el primer dia--.
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar la contrasena propia")
    public ResponseEntity<ApiResponse<Void>> cambiarMiContrasena(
            @Valid @RequestBody CambioContrasenaRequest request) {
        userService.cambiarMiContrasena(SecurityUtils.currentUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada", null));
    }

    /**
     * Escoger el roboticito propio. Sin rol: lo usan los cinco.
     *
     * El cuerpo es {"avatar": "bot-luna"}; mandarlo vacio lo quita y la
     * pantalla vuelve a las iniciales. La lista de validos la tiene el
     * servidor, no el cliente.
     */
    @PutMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Escoger el avatar propio")
    public ResponseEntity<ApiResponse<UserResponse>> cambiarMiAvatar(
            @RequestBody Map<String, String> cuerpo) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.cambiarMiAvatar(SecurityUtils.currentUsername(), cuerpo.get("avatar"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(userService.findById(id))));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','TEACHER')")
    @Operation(summary = "Listar usuarios por rol")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findByRole(role)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar usuario")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado", null));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Usuario creado", userService.create(request)));
    }

    @PostMapping("/{parentId}/link-student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vincular padre con alumno")
    public ResponseEntity<ApiResponse<Void>> linkStudent(@PathVariable UUID parentId,
                                                         @PathVariable UUID studentId,
                                                         @RequestParam(defaultValue = "padre") String relationshipType) {
        User parent = userService.findById(parentId);
        User student = userService.findById(studentId);

        if (parentStudentRepository.findByParentAndStudent(parent, student).isPresent()) {
            throw new BusinessException("Ya existe ese vinculo padre-alumno");
        }

        parentStudentRepository.save(
                ParentStudent.builder()
                        .parent(parent)
                        .student(student)
                        .relationshipType(relationshipType)
                        .build()
        );
        return ResponseEntity.status(201).body(ApiResponse.ok("Vinculo creado", null));
    }

    @GetMapping("/{parentId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','PARENT')")
    @Operation(summary = "Alumnos vinculados a un padre")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStudents(@PathVariable UUID parentId) {
        User parent = userService.findById(parentId);
        List<UserResponse> students = parentStudentRepository.findChildrenByParent(parent)
                .stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @DeleteMapping("/{parentId}/link-student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desvincular padre de alumno")
    public ResponseEntity<ApiResponse<Void>> unlinkStudent(@PathVariable UUID parentId, @PathVariable UUID studentId) {
        User parent = userService.findById(parentId);
        User student = userService.findById(studentId);
        parentStudentRepository.findByParentAndStudent(parent, student)
                .ifPresent(parentStudentRepository::delete);
        return ResponseEntity.ok(ApiResponse.ok("Vinculo eliminado", null));
    }
}
