package com.taskhabit.api.reporte.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de ENTRADA para generar (POST) o actualizar (PUT) un reporte. Ejemplo:
 * { "titulo": "Semana 4", "fecha_inicio": "2026-09-22", "fecha_fin": "2026-09-28",
 *   "observaciones": "Me atrasé con los hábitos" }
 * Los totales NO vienen del cliente: los calcula el servidor desde la BD (no se pueden inventar).
 */
public record ReportePeticion(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título admite máximo 150 caracteres")
        String titulo,

        @NotNull(message = "La fecha de inicio es obligatoria (formato AAAA-MM-DD)")
        @JsonProperty("fecha_inicio")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria (formato AAAA-MM-DD)")
        @JsonProperty("fecha_fin")
        LocalDate fechaFin,

        @Size(max = 1000, message = "Las observaciones admiten máximo 1000 caracteres")
        String observaciones
) {
}
