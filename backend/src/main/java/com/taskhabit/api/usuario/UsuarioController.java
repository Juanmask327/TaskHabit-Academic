package com.taskhabit.api.usuario;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taskhabit.api.usuario.dto.ActualizarPerfilPeticion;
import com.taskhabit.api.usuario.dto.UsuarioRespuesta;

import jakarta.validation.Valid;

/**
 * CONTROLADOR REST del módulo Usuarios. Traduce HTTP <-> llamadas al servicio.
 *
 * @RestController = @Controller + @ResponseBody: lo que devuelven los métodos se convierte a JSON.
 * @RequestMapping("/usuarios"): prefijo de todas las rutas. Con el context-path /api queda /api/usuarios.
 *
 * Endpoints:
 *   GET    /usuarios/me      -> mi perfil (lo crea si es la primera vez)       [Create + Read]
 *   PATCH  /usuarios/me      -> actualizar mi nombre/rol                        [Update]
 *   DELETE /usuarios/me      -> desactivar mi cuenta (borrado lógico)           [Delete]
 *   GET    /usuarios?rol=... -> listar usuarios activos (solo docentes)         [Read]
 *   GET    /usuarios/{id}    -> ver un usuario                                  [Read]
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    // Se aceptan "/me" y "/me/" porque la app Android llama con la barra final (usuarios/me/).
    // @AuthenticationPrincipal inyecta el token ya validado por Spring Security.
    @GetMapping({"/me", "/me/"})
    public UsuarioRespuesta miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return servicio.miPerfil(jwt); // 200 OK + JSON
    }

    // @RequestBody: convierte el JSON del cuerpo en el record. @Valid: aplica sus validaciones.
    @PatchMapping({"/me", "/me/"})
    public UsuarioRespuesta actualizarMiPerfil(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ActualizarPerfilPeticion peticion) {
        return servicio.actualizarMiPerfil(jwt, peticion);
    }

    @DeleteMapping({"/me", "/me/"})
    public ResponseEntity<Void> desactivarMiCuenta(@AuthenticationPrincipal Jwt jwt) {
        servicio.desactivarMiCuenta(jwt);
        return ResponseEntity.noContent().build(); // 204 No Content: éxito sin cuerpo
    }

    // @RequestParam(required = false): ?rol=ESTUDIANTE es opcional.
    @GetMapping
    public List<UsuarioRespuesta> listar(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) Rol rol) {
        return servicio.listar(jwt, rol);
    }

    // @PathVariable: toma el {id} de la URL y lo convierte a Long.
    @GetMapping("/{id}")
    public UsuarioRespuesta obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.obtener(jwt, id);
    }
}
