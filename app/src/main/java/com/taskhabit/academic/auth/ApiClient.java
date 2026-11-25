package com.taskhabit.academic.auth;

import com.taskhabit.academic.BuildConfig;
import com.taskhabit.academic.reportes.ReporteApi;
import com.taskhabit.academic.tareas.TareaApi;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Punto único para hablar con el API de TaskHabit Academic (backend Spring Boot).
 *
 * La dirección NO está escrita aquí: sale de BuildConfig.API_URL (ver app/build.gradle.kts).
 * Por defecto es http://127.0.0.1:8000/api/: el propio dispositivo, cuyo puerto 8000 la tarea
 * de Gradle "adbReverse" conecta con el del PC por adb (USB o depuración inalámbrica).
 * Se puede cambiar por computador con "taskhabit.api.url" en local.properties.
 */
public final class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() { }

    public static Retrofit obtener() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // El detalle de cada petición solo se imprime en las compilaciones de depuración.
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient cliente = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_URL)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static UsuarioApi usuarios() {
        return obtener().create(UsuarioApi.class);
    }

    public static TareaApi tareas() {
        return obtener().create(TareaApi.class);
    }

    public static ReporteApi reportes() {
        return obtener().create(ReporteApi.class);
    }
}
