package com.taskhabit.api.usuario.dto;

import com.taskhabit.api.usuario.Rol;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de ENTRADA para PATCH /usuarios/me. Ambos campos son opcionales:
 * PATCH = actualización PARCIAL, solo cambia lo que venga (null = "no tocar").
 * Es lo que envía RegisterActivity en la app: {"nombre": "...", "rol": "DOCENTE"}.
 *
 * Las anotaciones de validación se revisan cuando el controlador usa @Valid.
 */
public record ActualizarPerfilPeticion(
        // Si viene, debe tener entre 2 y 120 caracteres y contener algo que no sea espacio.
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        String nombre,

        // Jackson convierte "DOCENTE" -> Rol.DOCENTE. Un valor inválido da 400 (ver ManejadorErrores).
        Rol rol
) {
}
