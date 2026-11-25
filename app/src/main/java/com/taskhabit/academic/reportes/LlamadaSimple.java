package com.taskhabit.academic.reportes;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback de Retrofit reducido a dos casos: éxito (con el cuerpo) o error (sin conexión
 * o respuesta 4xx/5xx). Evita repetir onResponse/onFailure en cada pantalla de reportes.
 * Retrofit ya entrega ambos métodos en el hilo principal.
 */
abstract class LlamadaSimple<T> implements Callback<T> {

    abstract void onExito(T cuerpo);

    abstract void onError();

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> resp) {
        if (resp.isSuccessful()) {
            onExito(resp.body());
        } else {
            onError();
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        onError();
    }
}
