package com.taskhabit.api.usuario;

/**
 * Roles posibles. Un enum garantiza en Java lo mismo que el CHECK garantiza en la BD:
 * no puede existir un rol distinto a estos. Se guarda como texto ('DOCENTE') en la columna rol.
 */
public enum Rol {
    DOCENTE,
    ESTUDIANTE
}
