package mx.bytekids.academy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.common.ApiResponse;
import mx.bytekids.academy.dto.comunidad.ComunidadSalonResponse;
import mx.bytekids.academy.security.SecurityUtils;
import mx.bytekids.academy.service.ComunidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * La Comunidad vista por el maestro. El alumno tiene la suya en
 * /progress/leaderboard/mi-salon y /achievements/mi-salon/recientes, que se
 * acotan a SU salon; esto se acota a los salones del maestro.
 *
 * El permiso por salon lo revisa ComunidadService: aqui solo el rol.
 */
@RestController
@RequestMapping("/comunidad")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN','DIRECTOR')")
@Tag(name = "Comunidad del maestro")
public class ComunidadController {

    private final ComunidadService comunidadService;

    @GetMapping("/salones")
    @Operation(summary = "Los salones cuyo muro puedo ver")
    public ResponseEntity<ApiResponse<List<ComunidadSalonResponse.Salon>>> salones() {
        return ResponseEntity.ok(ApiResponse.ok(comunidadService.misSalones(SecurityUtils.currentUsername())));
    }

    @GetMapping("/salones/{salonId}")
    @Operation(summary = "El muro de un salon: logros recientes, ranking y quien no ha empezado")
    public ResponseEntity<ApiResponse<ComunidadSalonResponse>> muro(@PathVariable UUID salonId) {
        return ResponseEntity.ok(ApiResponse.ok(comunidadService.muro(SecurityUtils.currentUsername(), salonId)));
    }

    @PostMapping("/felicitar/{logroId}")
    @Operation(summary = "Felicitar a un alumno por un logro")
    public ResponseEntity<ApiResponse<Boolean>> felicitar(@PathVariable UUID logroId) {
        boolean nueva = comunidadService.felicitar(SecurityUtils.currentUsername(), logroId);
        return ResponseEntity.ok(ApiResponse.ok(
                nueva ? "Felicitación enviada" : "Ya lo habías felicitado", nueva));
    }
}
