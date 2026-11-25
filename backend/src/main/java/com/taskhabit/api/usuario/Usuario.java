package com.taskhabit.api.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * ENTIDAD JPA: esta clase "es" la tabla usuarios. Cada objeto Usuario = una fila.
 *
 * Hibernate (la implementación de JPA) lee estas anotaciones para saber cómo convertir
 * objeto <-> fila. Nosotros trabajamos con objetos; Hibernate escribe el SQL.
 */
@Entity                      // Marca la clase como entidad administrada por JPA.
@Table(name = "usuarios")    // Nombre exacto de la tabla (creada en V1__esquema_inicial.sql).
public class Usuario {

    @Id                                                   // Llave primaria.
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // La genera PostgreSQL (GENERATED ... AS IDENTITY).
    private Long id;

    // @Column enlaza el atributo con su columna. nullable/unique/length documentan la restricción;
    // quien realmente la hace cumplir es la BD.
    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false, length = 120)
    private String nombre;

    // EnumType.STRING guarda el NOMBRE ("DOCENTE"), no la posición (0, 1).
    // Si se guardara la posición y algún día se reordena el enum, los datos quedarían corruptos.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    // updatable = false: Hibernate nunca incluirá esta columna en un UPDATE.
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    /** JPA exige un constructor sin argumentos para crear objetos al leer filas. protected = no se usa fuera. */
    protected Usuario() {
    }

    public Usuario(String firebaseUid, String correo, String nombre, Rol rol) {
        this.firebaseUid = firebaseUid;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    /** Callback de JPA: se ejecuta justo ANTES del INSERT. Pone la fecha de registro automáticamente. */
    @PrePersist
    void antesDeInsertar() {
        this.fechaRegistro = Instant.now();
    }

    public boolean esDocente() {
        return rol == Rol.DOCENTE;
    }

    // --- Getters y setters (JPA y nuestro código acceden a los datos por aquí) ---
    // No hay setter para id, firebaseUid ni fechaRegistro: no deben cambiar nunca.

    public Long getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getFechaRegistro() { return fechaRegistro; }
}
