package com.taskhabit.academic.tareas;

import com.google.gson.annotations.SerializedName;

/**
 * Espejo del JSON de tareas del backend (TareaPeticion / TareaRespuesta).
 * Se usa para enviar y para recibir: Gson omite los campos null al enviar, así que
 * id y fecha_creacion (que pone el servidor) no viajan en un POST o PUT.
 */
public class TareaRemota {

    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private String prioridad;
    private Boolean completada;

    @SerializedName("fecha_creacion")
    private String fechaCreacion; // ISO-8601, ej. "2026-09-27T18:29:08.123Z"

    /** Arma el cuerpo de un POST/PUT a partir de la tarea local. */
    public static TareaRemota desde(Tarea local) {
        TareaRemota r = new TareaRemota();
        r.titulo = local.getTitulo();
        r.descripcion = local.getDescripcion();
        r.tipo = local.getTipo();
        r.prioridad = local.getPrioridad();
        r.completada = local.isCompletada();
        return r;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getTipo() { return tipo; }
    public String getPrioridad() { return prioridad; }
    public boolean isCompletada() { return Boolean.TRUE.equals(completada); }
    public String getFechaCreacion() { return fechaCreacion; }
}
