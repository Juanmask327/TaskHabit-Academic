package com.taskhabit.api.tarea;

import java.time.Instant;

import com.taskhabit.api.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * ENTIDAD Tarea -> tabla tareas. Sirve tanto para tareas como para hábitos (campo tipo),
 * igual que la entidad Room de la app, para que sincronizar sea directo.
 */
@Entity
@Table(name = "tareas")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIÓN muchos-a-uno: muchas tareas pertenecen a un usuario.
     * En la BD es la llave foránea usuario_id; en Java es un objeto Usuario completo.
     *
     * FetchType.LAZY: al leer una tarea NO se hace el JOIN con usuarios de inmediato;
     * Hibernate solo carga el usuario si alguien lo pide (ahorra consultas).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTarea tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Column(nullable = false)
    private boolean completada;

    @Column(name = "fecha_entrega")
    private Instant fechaEntrega;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected Tarea() {
    }

    public Tarea(Usuario usuario) {
        this.usuario = usuario;
    }

    /** Antes del INSERT: ambas fechas = ahora. */
    @PrePersist
    void antesDeInsertar() {
        this.fechaCreacion = Instant.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    /** Antes de cada UPDATE: se renueva la fecha de actualización. */
    @PreUpdate
    void antesDeActualizar() {
        this.fechaActualizacion = Instant.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoTarea getTipo() { return tipo; }
    public void setTipo(TipoTarea tipo) { this.tipo = tipo; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
    public Instant getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(Instant fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
}
