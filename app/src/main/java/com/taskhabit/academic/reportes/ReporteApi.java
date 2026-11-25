package com.taskhabit.academic.reportes;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Endpoints de ReporteController (backend). A diferencia de las tareas, los reportes
 * NO se guardan en Room: los calcula el servidor, así que necesitan conexión.
 */
public interface ReporteApi {

    @GET("reportes")
    Call<List<Reporte>> listar();

    @GET("reportes/{id}")
    Call<Reporte> obtener(@Path("id") long id);

    @POST("reportes")
    Call<Reporte> generar(@Body ReportePeticion peticion);

    @PUT("reportes/{id}")
    Call<Reporte> actualizar(@Path("id") long id, @Body ReportePeticion peticion);

    @DELETE("reportes/{id}")
    Call<Void> eliminar(@Path("id") long id);
}
