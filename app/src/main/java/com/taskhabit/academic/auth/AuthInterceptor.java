package com.taskhabit.academic.auth;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Le pone "Authorization: Bearer <idToken>" a cada petición, automáticamente.
 *
 * Tasks.await() bloquea el hilo actual hasta tener el token — está bien aquí porque
 * OkHttp ya corre los interceptors en su propio hilo de fondo, nunca en el hilo principal.
 * getIdToken(false) reusa el token vigente sin red si no ha vencido (dura ~1 hora);
 * Firebase lo renueva solo cuando hace falta.
 */
public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            // Nadie ha iniciado sesión: se manda la petición tal cual: el API la
            // rechazará con 401, y quien llamó a Retrofit debe redirigir a Login.
            return chain.proceed(original);
        }

        String token;
        try {
            GetTokenResult resultado = Tasks.await(usuario.getIdToken(false), 10, TimeUnit.SECONDS);
            token = resultado.getToken();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException("No se pudo obtener el token de Firebase", e);
        }

        Request conToken = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(conToken);
    }
}
