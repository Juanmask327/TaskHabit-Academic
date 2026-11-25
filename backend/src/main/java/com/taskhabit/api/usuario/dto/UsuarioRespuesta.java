package com.taskhabit.api.usuario.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskhabit.api.usuario.Rol;
import com.taskhabit.api.usuario.Usuario;

/**
 * DTO (Data Transfer Object) de SALIDA: lo que el API devuelve en JSON.
 *
 * ¿Por qué no devolver la entidad directamente?
 *  - Controlamos exactamente qué campos salen (la entidad podría tener datos internos).
 *  - Si cambia la tabla, el contrato con la app Android no se rompe.
 *  - Evitamos errores de carga "lazy" de Hibernate al serializar.
 *
 * @JsonProperty fija el nombre en el JSON. La app Android espera snake_case
 * (ver Usuario.java en la app: @SerializedName("firebase_uid")).
 */
public record UsuarioRespuesta(
        Long id,
        @JsonProperty("firebase_uid") String firebaseUid,
        String correo,
        String nombre,
        Rol rol,
        boolean activo,
        @JsonProperty("fecha_registro") Instant fechaRegistro
) {

    /** Convierte la entidad (fila de BD) en el DTO (JSON). */
    public static UsuarioRespuesta desde(Usuario u) {
        return new UsuarioRespuesta(
                u.getId(), u.getFirebaseUid(), u.getCorreo(), u.getNombre(),
                u.getRol(), u.isActivo(), u.getFechaRegistro());
    }
}
