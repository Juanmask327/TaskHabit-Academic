package com.taskhabit.academic.tareas;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Punto 2 del acta: CRUD local con base de datos (Room, sobre SQLite), arquitectura offline-first.
 * La app SIEMPRE lee y escribe aquí (funciona sin internet); SincronizadorTareas copia los
 * cambios al backend cuando hay conexión. Para eso cada fila recuerda:
 *   - idRemoto:  el id que le dio PostgreSQL (null = todavía no existe en el servidor)
 *   - pendiente: true = tiene cambios locales que aún no se han subido
 *   - eliminada: true = el usuario la borró, pero falta avisarle al servidor
 */
@Entity(tableName = "tareas")
public class Tarea {

    public static final String TIPO_TAREA = "TAREA";
    public static final String TIPO_HABITO = "HABITO";

    public static final String PRIORIDAD_BAJA = "BAJA";
    public static final String PRIORIDAD_MEDIA = "MEDIA";
    public static final String PRIORIDAD_ALTA = "ALTA";

    @PrimaryKey(autoGenerate = true)
    private long id;

    // El uid de Firebase de quien creó la tarea — así cada cuenta ve solo lo suyo,
    // aunque varias personas usen la app en el mismo celular.
    @ColumnInfo(name = "usuario_uid")
    private String usuarioUid;

    @ColumnInfo(name = "titulo")
    private String titulo;

    @ColumnInfo(name = "descripcion")
    private String descripcion;

    @ColumnInfo(name = "tipo")
    private String tipo;

    @ColumnInfo(name = "prioridad")
    private String prioridad;

    @ColumnInfo(name = "completada")
    private boolean completada;

    @ColumnInfo(name = "fecha_creacion")
    private long fechaCreacion;

    // Long (objeto) para poder guardar NULL: la tarea aún no se ha subido al servidor.
    @ColumnInfo(name = "id_remoto")
    private Long idRemoto;

    // Toda tarea nueva nace pendiente de subir.
    @ColumnInfo(name = "pendiente")
    private boolean pendiente = true;

    // Borrado "suave": se oculta de la lista pero la fila sigue hasta que el servidor confirme.
    @ColumnInfo(name = "eliminada")
    private boolean eliminada;

    public Tarea(String usuarioUid, String titulo, String descripcion, String tipo, String prioridad,
                 boolean completada, long fechaCreacion) {
        this.usuarioUid = usuarioUid;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.completada = completada;
        this.fechaCreacion = fechaCreacion;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsuarioUid() { return usuarioUid; }
    public void setUsuarioUid(String usuarioUid) { this.usuarioUid = usuarioUid; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Long getIdRemoto() { return idRemoto; }
    public void setIdRemoto(Long idRemoto) { this.idRemoto = idRemoto; }
    public boolean isPendiente() { return pendiente; }
    public void setPendiente(boolean pendiente) { this.pendiente = pendiente; }
    public boolean isEliminada() { return eliminada; }
    public void setEliminada(boolean eliminada) { this.eliminada = eliminada; }

    public boolean esHabito() { return TIPO_HABITO.equals(tipo); }
}
