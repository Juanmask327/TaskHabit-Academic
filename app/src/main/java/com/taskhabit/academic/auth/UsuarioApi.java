package com.taskhabit.academic.auth;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Body;

/**
 * Endpoints de UsuarioController (backend Spring Boot).
 * /usuarios/me/ es sobre uno mismo: el backend identifica al usuario por el token,
 * nunca hay que mandar un id.
 */
public interface UsuarioApi {

    @GET("usuarios/me/")
    Call<Usuario> obtenerMiPerfil();

    @PATCH("usuarios/me/")
    Call<Usuario> actualizarMiPerfil(@Body Map<String, Object> cambios);
}
