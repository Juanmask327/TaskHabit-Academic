package com.taskhabit.academic.reportes;

import com.google.gson.annotations.SerializedName;

/**
 * Espejo de ReporteRespuesta del backend. Los totales los calcula el servidor
 * contando las tareas del periodo en PostgreSQL; la app solo los muestra.
 */
public class Reporte {

    private long id;
    private String titulo;

    @SerializedName("fecha_inicio")
    private String fechaInicio; // "AAAA-MM-DD"

    @SerializedName("fecha_fin")
    private String fechaFin;

    @SerializedName("total_tareas")
    private int totalTareas;

    @SerializedName("tareas_completadas")
    private int tareasCompletadas;

    @SerializedName("total_habitos")
    private int totalHabitos;

    @SerializedName("habitos_completados")
    private int habitosCompletados;

    @SerializedName("porcentaje_cumplimiento")
    private double porcentajeCumplimiento;

    private String observaciones;

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public int getTotalTareas() { return totalTareas; }
    public int getTareasCompletadas() { return tareasCompletadas; }
    public int getTotalHabitos() { return totalHabitos; }
    public int getHabitosCompletados() { return habitosCompletados; }
    public double getPorcentajeCumplimiento() { return porcentajeCumplimiento; }
    public String getObservaciones() { return observaciones; }
}
