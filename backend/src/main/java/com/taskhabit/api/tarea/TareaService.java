package com.taskhabit.api.tarea;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskhabit.api.comun.RecursoNoEncontradoException;
import com.taskhabit.api.tarea.dto.TareaPeticion;
import com.taskhabit.api.tarea.dto.TareaRespuesta;
import com.taskhabit.api.usuario.Usuario;
import com.taskhabit.api.usuario.UsuarioService;

/**
 * Lógica del CRUD de tareas. Regla principal: cada usuario solo ve y modifica LO SUYO.
 */
@Service
public class TareaService {

    private final TareaRepository tareas;
    private final UsuarioService usuarioService;

    public TareaService(TareaRepository tareas, UsuarioService usuarioService) {
        this.tareas = tareas;
        this.usuarioService = usuarioService;
    }

    /**
     * CREATE -> INSERT INTO tareas (...) VALUES (...)
     * usuarioService.usuarioActual() también es @Transactional: al llamarlo desde aquí se UNE
     * a esta misma transacción (propagación REQUIRED por defecto), no abre otra.
     */
    @Transactional
    public TareaRespuesta crear(Jwt jwt, TareaPeticion peticion) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        Tarea nueva = new Tarea(yo);           // el dueño sale del token, nunca del JSON
        copiarDatos(peticion, nueva);
        // save() con id null -> INSERT. PostgreSQL genera el id y Hibernate lo coloca en el objeto.
        Tarea guardada = tareas.save(nueva);
        return TareaRespuesta.desde(guardada);
    }

    /** READ (lista) -> SELECT ... WHERE usuario_id = ? [AND tipo = ?] [AND completada = ?] */
    @Transactional
    public List<TareaRespuesta> listar(Jwt jwt, TipoTarea tipo, Boolean completada) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        return tareas.buscar(yo, tipo, completada).stream()
                .map(TareaRespuesta::desde)
                .toList();
    }

    /** READ (una) -> SELECT ... WHERE id = ? AND usuario_id = ? */
    @Transactional
    public TareaRespuesta obtener(Jwt jwt, Long id) {
        return TareaRespuesta.desde(buscarPropia(jwt, id));
    }

    /**
     * UPDATE (PUT = reemplazo completo) -> UPDATE tareas SET ... WHERE id = ?
     * No llamamos save(): la tarea está administrada por Hibernate dentro de la transacción,
     * así que al hacer COMMIT detecta los cambios (dirty checking) y ejecuta el UPDATE.
     * Antes del UPDATE corre @PreUpdate y renueva fecha_actualizacion.
     */
    @Transactional
    public TareaRespuesta actualizar(Jwt jwt, Long id, TareaPeticion peticion) {
        Tarea tarea = buscarPropia(jwt, id);
        copiarDatos(peticion, tarea);
        return TareaRespuesta.desde(tarea);
    }

    /** UPDATE parcial: solo marca/desmarca completada (checkbox de la lista). */
    @Transactional
    public TareaRespuesta cambiarCompletada(Jwt jwt, Long id, boolean completada) {
        Tarea tarea = buscarPropia(jwt, id);
        tarea.setCompletada(completada);
        return TareaRespuesta.desde(tarea);
    }

    /** DELETE -> DELETE FROM tareas WHERE id = ?  (borrado físico: la fila desaparece). */
    @Transactional
    public void eliminar(Jwt jwt, Long id) {
        Tarea tarea = buscarPropia(jwt, id);
        tareas.delete(tarea);
    }

    /** Busca la tarea filtrando también por dueño; si no aparece -> 404. */
    private Tarea buscarPropia(Jwt jwt, Long id) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        return tareas.findByIdAndUsuario(id, yo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea", id));
    }

    /** Pasa los datos del DTO a la entidad (se reutiliza en crear y actualizar). */
    private static void copiarDatos(TareaPeticion p, Tarea t) {
        t.setTitulo(p.titulo().trim());
        t.setDescripcion(p.descripcion());
        t.setTipo(p.tipo());
        t.setPrioridad(p.prioridad());
        t.setCompletada(Boolean.TRUE.equals(p.completada())); // null -> false
        t.setFechaEntrega(p.fechaEntrega());
    }
}
