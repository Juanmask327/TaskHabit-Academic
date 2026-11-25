package com.taskhabit.api.reporte;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.taskhabit.api.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ENTIDAD Reporte -> tabla reportes.
 * Un reporte resume el desempeño de un usuario (tareas y hábitos) en un periodo.
 * Los totales se calculan con consultas a la tabla tareas y se GUARDAN, así el reporte
 * queda como una "foto" de ese momento aunque las tareas cambien después.
 */
@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación muchos-a-uno con el autor (llave foránea autor_id).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false, updatable = false)
    private Usuario autor;

    @Column(nullable = false, length = 150)
    private String titulo;

    // LocalDate = solo fecha (sin hora) -> columna DATE.
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "total_tareas", nullable = false)
    private int totalTareas;

    @Column(name = "tareas_completadas", nullable = false)
    private int tareasCompletadas;

    @Column(name = "total_habitos", nullable = false)
    private int totalHabitos;

    @Column(name = "habitos_completados", nullable = false)
    private int habitosCompletados;

    // BigDecimal (y no double) para decimales exactos: double podría guardar 87.4999999.
    // precision = dígitos totales, scale = decimales -> NUMERIC(5,2).
    @Column(name = "porcentaje_cumplimiento", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeCumplimiento;

    @Column(length = 1000)
    private String observaciones;

    // Se renueva cada vez que se recalculan los totales.
    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    protected Reporte() {
    }

    public Reporte(Usuario autor) {
        this.autor = autor;
    }

    @PrePersist
    @PreUpdate
    void marcarGeneracion() {
        this.fechaGeneracion = Instant.now();
    }

    public Long getId() { return id; }
    public Usuario getAutor() { return autor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public int getTotalTareas() { return totalTareas; }
    public void setTotalTareas(int totalTareas) { this.totalTareas = totalTareas; }
    public int getTareasCompletadas() { return tareasCompletadas; }
    public void setTareasCompletadas(int tareasCompletadas) { this.tareasCompletadas = tareasCompletadas; }
    public int getTotalHabitos() { return totalHabitos; }
    public void setTotalHabitos(int totalHabitos) { this.totalHabitos = totalHabitos; }
    public int getHabitosCompletados() { return habitosCompletados; }
    public void setHabitosCompletados(int habitosCompletados) { this.habitosCompletados = habitosCompletados; }
    public BigDecimal getPorcentajeCumplimiento() { return porcentajeCumplimiento; }
    public void setPorcentajeCumplimiento(BigDecimal porcentaje) { this.porcentajeCumplimiento = porcentaje; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Instant getFechaGeneracion() { return fechaGeneracion; }
}
