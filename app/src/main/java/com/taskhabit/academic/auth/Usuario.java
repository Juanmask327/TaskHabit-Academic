package com.taskhabit.academic.auth;

import com.google.gson.annotations.SerializedName;

/** Espejo de UsuarioRespuesta del backend (JSON en snake_case). */
public class Usuario {

    public static final String ROL_DOCENTE = "DOCENTE";
    public static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private Long id;

    @SerializedName("firebase_uid")
    private String firebaseUid;

    private String correo;
    private String nombre;
    private String rol;
    private boolean activo;

    @SerializedName("fecha_registro")
    private String fechaRegistro;

    public Long getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public String getFechaRegistro() { return fechaRegistro; }

    public boolean esDocente() { return ROL_DOCENTE.equals(rol); }
}
