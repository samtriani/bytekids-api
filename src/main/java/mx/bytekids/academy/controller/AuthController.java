package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.auth.LoginRequest;
import mx.bytekids.academy.dto.auth.LoginResponse;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.service.AuthService;
import mx.bytekids.academy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar nuevo usuario (solo admin)")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Usuario creado", userService.create(req)));
    }
}
