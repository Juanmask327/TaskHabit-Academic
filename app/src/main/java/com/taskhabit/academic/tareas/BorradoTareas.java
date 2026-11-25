package com.taskhabit.academic.tareas;

/**
 * Regla de borrado offline-first, compartida por la lista y el formulario.
 * Debe llamarse desde un hilo de fondo (DbExecutor).
 */
final class BorradoTareas {

    private BorradoTareas() { }

    static void marcarOEliminar(TareaDao dao, Tarea tarea) {
        if (tarea.getIdRemoto() == null) {
            // Nunca llegó al servidor: se puede borrar del todo ya.
            dao.eliminar(tarea);
        } else {
            // Existe en el servidor: se oculta y queda pendiente de avisarle (DELETE).
            tarea.setEliminada(true);
            tarea.setPendiente(true);
            dao.actualizar(tarea);
        }
    }
}
