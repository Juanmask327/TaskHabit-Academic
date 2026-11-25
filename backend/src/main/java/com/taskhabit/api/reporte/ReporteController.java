package com.taskhabit.api.reporte;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.taskhabit.api.reporte.dto.ReportePeticion;
import com.taskhabit.api.reporte.dto.ReporteRespuesta;

import jakarta.validation.Valid;

/**
 * CRUD REST de reportes de desempeño.
 *
 *   POST   /reportes       -> generar reporte de un periodo   201 Created
 *   GET    /reportes       -> mis reportes                    200 OK
 *   GET    /reportes/{id}  -> ver uno                         200 / 404
 *   PUT    /reportes/{id}  -> editar y recalcular             200 / 404
 *   DELETE /reportes/{id}  -> eliminar                        204 / 404
 */
@RestController
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ReporteRespuesta> generar(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody ReportePeticion peticion) {
        ReporteRespuesta creado = servicio.generar(jwt, peticion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public List<ReporteRespuesta> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(jwt);
    }

    @GetMapping("/{id}")
    public ReporteRespuesta obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.obtener(jwt, id);
    }

    @PutMapping("/{id}")
    public ReporteRespuesta actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody ReportePeticion peticion) {
        return servicio.actualizar(jwt, id, peticion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        servicio.eliminar(jwt, id);
        return ResponseEntity.noContent().build();
    }
}
