package com.taskhabit.api.comun;

/**
 * Se lanza cuando el usuario está autenticado pero no tiene permiso
 * (ej. un estudiante intenta listar a todos los usuarios). Se traduce a HTTP 403.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
