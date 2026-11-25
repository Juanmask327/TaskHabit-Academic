package com.taskhabit.academic.reportes;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de POST /reportes y PUT /reportes/{id}. Las fechas van como "AAAA-MM-DD". */
public class ReportePeticion {

    private final String titulo;

    @SerializedName("fecha_inicio")
    private final String fechaInicio;

    @SerializedName("fecha_fin")
    private final String fechaFin;

    private final String observaciones;

    public ReportePeticion(String titulo, String fechaInicio, String fechaFin, String observaciones) {
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.observaciones = observaciones;
    }
}
