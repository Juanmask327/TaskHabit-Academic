package com.taskhabit.api.tarea;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.taskhabit.api.tarea.dto.CompletadaPeticion;
import com.taskhabit.api.tarea.dto.TareaPeticion;
import com.taskhabit.api.tarea.dto.TareaRespuesta;

import jakarta.validation.Valid;

/**
 * CRUD REST de tareas y hábitos (del usuario autenticado).
 *
 *   POST   /tareas                    -> crear                         201 Created
 *   GET    /tareas?tipo=&completada=  -> listar (filtros opcionales)   200 OK
 *   GET    /tareas/{id}               -> ver una                       200 / 404
 *   PUT    /tareas/{id}               -> reemplazar                    200 / 404
 *   PATCH  /tareas/{id}/completada    -> marcar/desmarcar              200 / 404
 *   DELETE /tareas/{id}               -> eliminar                      204 / 404
 */
@RestController
@RequestMapping("/tareas")
public class TareaController {

    private final TareaService servicio;

    public TareaController(TareaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<TareaRespuesta> crear(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody TareaPeticion peticion) {
        TareaRespuesta creada = servicio.crear(jwt, peticion);
        // Buena práctica REST: responder 201 y el header Location con la URL del recurso nuevo.
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public List<TareaRespuesta> listar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) TipoTarea tipo,
                                       @RequestParam(required = false) Boolean completada) {
        return servicio.listar(jwt, tipo, completada);
    }

    @GetMapping("/{id}")
    public TareaRespuesta obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.obtener(jwt, id);
    }

    @PutMapping("/{id}")
    public TareaRespuesta actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody TareaPeticion peticion) {
        return servicio.actualizar(jwt, id, peticion);
    }

    @PatchMapping("/{id}/completada")
    public TareaRespuesta cambiarCompletada(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                            @Valid @RequestBody CompletadaPeticion peticion) {
        return servicio.cambiarCompletada(jwt, id, peticion.completada());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        servicio.eliminar(jwt, id);
        return ResponseEntity.noContent().build();
    }
}
