package com.taskhabit.api.tarea.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskhabit.api.tarea.Prioridad;
import com.taskhabit.api.tarea.TipoTarea;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de ENTRADA para crear (POST) o reemplazar (PUT) una tarea.
 * Ejemplo de JSON:
 * {
 *   "titulo": "Taller de cálculo", "descripcion": "Ejercicios 1-10",
 *   "tipo": "TAREA", "prioridad": "ALTA", "completada": false,
 *   "fecha_entrega": "2026-10-03T23:59:00Z"
 * }
 * Nótese que NO trae usuario_id: el dueño siempre es quien manda el token (no se puede falsificar).
 */
public record TareaPeticion(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título admite máximo 150 caracteres")
        String titulo,

        @Size(max = 1000, message = "La descripción admite máximo 1000 caracteres")
        String descripcion,

        @NotNull(message = "El tipo es obligatorio (TAREA o HABITO)")
        TipoTarea tipo,

        @NotNull(message = "La prioridad es obligatoria (BAJA, MEDIA o ALTA)")
        Prioridad prioridad,

        // Boolean (objeto) y no boolean: así podemos saber si NO vino (null) y usar false.
        Boolean completada,

        @JsonProperty("fecha_entrega")
        Instant fechaEntrega
) {
}
