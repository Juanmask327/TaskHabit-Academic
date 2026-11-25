package com.taskhabit.academic.tareas;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Endpoints de TareaController (backend). Retrofit genera la implementación:
 * cada método se convierte en una petición HTTP a BuildConfig.API_URL + la ruta.
 * El token de Firebase lo agrega AuthInterceptor; aquí no hay que mandarlo.
 */
public interface TareaApi {

    @GET("tareas")
    Call<List<TareaRemota>> listar();

    @POST("tareas")
    Call<TareaRemota> crear(@Body TareaRemota tarea);

    @PUT("tareas/{id}")
    Call<TareaRemota> actualizar(@Path("id") long id, @Body TareaRemota tarea);

    @DELETE("tareas/{id}")
    Call<Void> eliminar(@Path("id") long id);
}
