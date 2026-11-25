package com.taskhabit.api.tarea.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /tareas/{id}/completada: {"completada": true}. Para el checkbox de la lista. */
public record CompletadaPeticion(
        @NotNull(message = "Indica true o false")
        Boolean completada
) {
}
