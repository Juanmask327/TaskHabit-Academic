package com.taskhabit.api.reporte.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskhabit.api.reporte.Reporte;

/** DTO de SALIDA de un reporte. */
public record ReporteRespuesta(
        Long id,
        String titulo,
        @JsonProperty("fecha_inicio") LocalDate fechaInicio,
        @JsonProperty("fecha_fin") LocalDate fechaFin,
        @JsonProperty("total_tareas") int totalTareas,
        @JsonProperty("tareas_completadas") int tareasCompletadas,
        @JsonProperty("total_habitos") int totalHabitos,
        @JsonProperty("habitos_completados") int habitosCompletados,
        @JsonProperty("porcentaje_cumplimiento") BigDecimal porcentajeCumplimiento,
        String observaciones,
        @JsonProperty("fecha_generacion") Instant fechaGeneracion
) {

    public static ReporteRespuesta desde(Reporte r) {
        return new ReporteRespuesta(
                r.getId(), r.getTitulo(), r.getFechaInicio(), r.getFechaFin(),
                r.getTotalTareas(), r.getTareasCompletadas(), r.getTotalHabitos(), r.getHabitosCompletados(),
                r.getPorcentajeCumplimiento(), r.getObservaciones(), r.getFechaGeneracion());
    }
}
