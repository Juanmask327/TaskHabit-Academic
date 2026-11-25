package com.taskhabit.api.comun;

/**
 * Se lanza cuando se pide algo que no existe (o que no es del usuario).
 * ManejadorErrores la convierte en una respuesta HTTP 404.
 * Extiende RuntimeException para no obligar a declarar "throws" en cada método.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe");
    }
}
