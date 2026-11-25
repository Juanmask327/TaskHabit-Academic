package com.taskhabit.api.usuario;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskhabit.api.comun.AccesoDenegadoException;
import com.taskhabit.api.comun.RecursoNoEncontradoException;
import com.taskhabit.api.usuario.dto.ActualizarPerfilPeticion;
import com.taskhabit.api.usuario.dto.UsuarioRespuesta;

/**
 * SERVICIO: aquí vive la lógica de negocio (reglas, permisos, transacciones).
 * El controlador solo recibe HTTP y delega aquí; el repositorio solo accede a la BD.
 *
 * @Service lo registra como bean para que Spring lo inyecte donde se necesite.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;

    /**
     * Inyección de dependencias por constructor: Spring ve que necesitamos un
     * UsuarioRepository y nos pasa la implementación que generó. Nunca hacemos "new".
     */
    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Devuelve el Usuario dueño del token. Si es la primera vez que ese usuario de Firebase
     * llama al API, lo CREA (auto-aprovisionamiento) con rol ESTUDIANTE por defecto.
     * Esto es el "C" (Create) del CRUD de usuarios: la cuenta nace en Firebase y la fila
     * en PostgreSQL se crea en su primera petición.
     *
     * Lo usan también TareaService y ReporteService para saber quién hace la petición.
     *
     * @Transactional: todo el método corre en UNA transacción de BD (BEGIN ... COMMIT).
     * Si ocurre una excepción, se hace ROLLBACK y nada queda a medias.
     */
    @Transactional
    public Usuario usuarioActual(Jwt jwt) {
        String uid = jwt.getSubject();                        // "sub" del token = uid de Firebase
        String correo = jwt.getClaimAsString("email");        // puede ser null en cuentas sin correo

        Usuario usuario = usuarios.findByFirebaseUid(uid)      // SELECT ... WHERE firebase_uid = ?
                .orElseGet(() -> usuarios.save(                // si no existe -> INSERT
                        new Usuario(uid, correoONombre(correo, uid), nombreInicial(jwt, correo), Rol.ESTUDIANTE)));

        if (!usuario.isActivo()) {
            throw new AccesoDenegadoException("La cuenta está desactivada");
        }

        // "Dirty checking": el objeto está ADMINISTRADO por Hibernate dentro de la transacción.
        // Si cambiamos un atributo, Hibernate detecta el cambio y hace el UPDATE solo al COMMIT,
        // sin llamar a save(). Así mantenemos el correo sincronizado con Firebase.
        if (correo != null && !correo.equals(usuario.getCorreo())) {
            usuario.setCorreo(correo);
        }
        return usuario;
    }

    /** READ: GET /usuarios/me */
    @Transactional
    public UsuarioRespuesta miPerfil(Jwt jwt) {
        return UsuarioRespuesta.desde(usuarioActual(jwt));
    }

    /** UPDATE: PATCH /usuarios/me. Solo cambia los campos que vienen (no null). */
    @Transactional
    public UsuarioRespuesta actualizarMiPerfil(Jwt jwt, ActualizarPerfilPeticion peticion) {
        Usuario yo = usuarioActual(jwt);
        if (peticion.nombre() != null) {
            yo.setNombre(peticion.nombre().trim());
        }
        // NOTA de diseño: el rol lo elige el propio usuario al registrarse (así funciona la app hoy).
        // En producción convendría que solo un administrador pueda asignar el rol DOCENTE.
        if (peticion.rol() != null) {
            yo.setRol(peticion.rol());
        }
        // Sin save(): el dirty checking genera el UPDATE al terminar la transacción.
        return UsuarioRespuesta.desde(yo);
    }

    /**
     * DELETE: DELETE /usuarios/me. Borrado LÓGICO: marca activo = false (UPDATE, no DELETE).
     * Así se conservan sus tareas y reportes para el historial académico.
     */
    @Transactional
    public void desactivarMiCuenta(Jwt jwt) {
        usuarioActual(jwt).setActivo(false);
    }

    /**
     * READ (lista): GET /usuarios?rol=ESTUDIANTE. Solo para docentes.
     * Sin readOnly: usuarioActual() puede hacer INSERT (primera petición) o UPDATE (correo), y
     * PostgreSQL prohíbe escribir dentro de una transacción de solo lectura.
     */
    @Transactional
    public List<UsuarioRespuesta> listar(Jwt jwt, Rol rol) {
        exigirDocente(usuarioActual(jwt));
        List<Usuario> resultado = (rol == null)
                ? usuarios.findByActivoTrueOrderByNombreAsc()
                : usuarios.findByRolAndActivoTrueOrderByNombreAsc(rol);
        // stream().map(...).toList(): convierte cada entidad a su DTO.
        return resultado.stream().map(UsuarioRespuesta::desde).toList();
    }

    /** READ (uno): GET /usuarios/{id}. Un docente ve a cualquiera; un estudiante solo a sí mismo. */
    @Transactional
    public UsuarioRespuesta obtener(Jwt jwt, Long id) {
        Usuario yo = usuarioActual(jwt);
        if (!yo.esDocente() && !yo.getId().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propio perfil");
        }
        Usuario buscado = usuarios.findById(id)                    // SELECT ... WHERE id = ?
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        return UsuarioRespuesta.desde(buscado);
    }

    private static void exigirDocente(Usuario usuario) {
        if (!usuario.esDocente()) {
            throw new AccesoDenegadoException("Solo los docentes pueden realizar esta acción");
        }
    }

    /** Nombre inicial: el "name" del token si existe; si no, la parte del correo antes de la @. */
    private static String nombreInicial(Jwt jwt, String correo) {
        String nombre = jwt.getClaimAsString("name");
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        return correo != null ? correo.substring(0, correo.indexOf('@')) : "Usuario";
    }

    private static String correoONombre(String correo, String uid) {
        return correo != null ? correo : uid + "@sin-correo";
    }
}
