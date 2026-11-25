package com.taskhabit.api.tarea.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskhabit.api.tarea.Prioridad;
import com.taskhabit.api.tarea.Tarea;
import com.taskhabit.api.tarea.TipoTarea;

/** DTO de SALIDA de una tarea (JSON en snake_case, igual que Usuario). */
public record TareaRespuesta(
        Long id,
        String titulo,
        String descripcion,
        TipoTarea tipo,
        Prioridad prioridad,
        boolean completada,
        @JsonProperty("fecha_entrega") Instant fechaEntrega,
        @JsonProperty("fecha_creacion") Instant fechaCreacion,
        @JsonProperty("fecha_actualizacion") Instant fechaActualizacion
) {

    public static TareaRespuesta desde(Tarea t) {
        return new TareaRespuesta(
                t.getId(), t.getTitulo(), t.getDescripcion(), t.getTipo(), t.getPrioridad(),
                t.isCompletada(), t.getFechaEntrega(), t.getFechaCreacion(), t.getFechaActualizacion());
    }
}
