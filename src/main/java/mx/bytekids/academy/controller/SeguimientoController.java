package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.seguimiento.SeguimientoResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.SeguimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Como va cada alumno de un salon, para el panel del maestro. El permiso por
 * salon (solo los suyos; coordinacion y direccion, todos) lo revisa el
 * servicio con la misma regla que la Comunidad.
 */
@RestController
@RequestMapping("/seguimiento")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN','DIRECTOR')")
@Tag(name = "Seguimiento de alumnos")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    @GetMapping("/salones/{salonId}")
    @Operation(summary = "Avance y alertas de cada alumno de un salon")
    public ResponseEntity<ApiResponse<SeguimientoResponse>> deSalon(@PathVariable UUID salonId) {
        return ResponseEntity.ok(ApiResponse.ok(
                seguimientoService.deSalon(SecurityUtils.currentUsername(), salonId)));
    }
}
