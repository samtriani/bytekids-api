package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Avance;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Detalle;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Fila;
import mx.bytekids.academy.dto.certificado.CertificadoDtos.Verificacion;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.CertificadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Ver CertificadoService: ahi estan las reglas de quien pide, entrega y ve. */
@RestController
@RequestMapping("/certificados")
@RequiredArgsConstructor
@Tag(name = "Certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    @GetMapping("/mios")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cómo voy hacia el certificado de cada materia")
    public ResponseEntity<ApiResponse<List<Avance>>> mios() {
        return ResponseEntity.ok(ApiResponse.ok(certificadoService.misAvances(SecurityUtils.currentUsername())));
    }

    @PostMapping("/solicitar/{materiaId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Pedir el certificado de una materia terminada")
    public ResponseEntity<ApiResponse<Avance>> solicitar(@PathVariable UUID materiaId) {
        return ResponseEntity.ok(ApiResponse.ok("Solicitud enviada",
                certificadoService.solicitar(SecurityUtils.currentUsername(), materiaId)));
    }

    @GetMapping("/de-mis-alumnos")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','DIRECTOR')")
    @Operation(summary = "Los certificados de mis alumnos, pendientes primero")
    public ResponseEntity<ApiResponse<List<Fila>>> deMisAlumnos() {
        return ResponseEntity.ok(ApiResponse.ok(certificadoService.deMisAlumnos(SecurityUtils.currentUsername())));
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','DIRECTOR')")
    @Operation(summary = "Entregar un certificado: le llega al alumno y a su familia")
    public ResponseEntity<ApiResponse<Fila>> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Certificado entregado",
                certificadoService.entregar(SecurityUtils.currentUsername(), id)));
    }

    /** Publico (ver SecurityConfig): es lo que abre el QR del certificado. */
    @GetMapping("/verificar/{folio}")
    @Operation(summary = "Verificar un certificado por su folio, sin iniciar sesión")
    public ResponseEntity<ApiResponse<Verificacion>> verificar(@PathVariable String folio) {
        return ResponseEntity.ok(ApiResponse.ok(certificadoService.verificar(folio)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Un certificado, para verlo e imprimirlo")
    public ResponseEntity<ApiResponse<Detalle>> detalle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(certificadoService.detalle(SecurityUtils.currentUsername(), id)));
    }
}
