package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.familia.HijoResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.FamiliaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El modulo de familias. Sin ids en la ruta, a proposito: devuelve los hijos
 * de quien pregunta y no hay forma de pedir los de otro.
 */
@RestController
@RequestMapping("/familia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Familia")
public class FamiliaController {

    private final FamiliaService familiaService;

    @GetMapping("/hijos")
    @Operation(summary = "Mis hijos: avance, camino por materia, logros, certificados y clases")
    public ResponseEntity<ApiResponse<List<HijoResponse>>> hijos() {
        return ResponseEntity.ok(ApiResponse.ok(familiaService.misHijos(SecurityUtils.currentUsername())));
    }

    @GetMapping("/hijos/{hijoId}/trabajos")
    @Operation(summary = "Lo que entregó un hijo, con la calificación y el comentario del maestro")
    public ResponseEntity<ApiResponse<List<mx.bytekids.academy.dto.familia.TrabajoResponse>>> trabajos(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID hijoId) {
        return ResponseEntity.ok(ApiResponse.ok(familiaService.trabajos(SecurityUtils.currentUsername(), hijoId)));
    }
}
