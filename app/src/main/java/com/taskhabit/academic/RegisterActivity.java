package com.taskhabit.academic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.taskhabit.academic.auth.ApiClient;
import com.taskhabit.academic.auth.Usuario;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registro con Firebase + sincronización con el API.
 *
 * Firebase por sí solo no sabe si la persona es docente o estudiante — ese dato vive
 * en nuestra base (usuarios.Usuario.rol). Por eso, justo después de crear la cuenta:
 *   1. Se guarda el nombre en el perfil de Firebase (solo de adorno, no lo usa el API).
 *   2. Se llama GET /usuarios/me/ una vez: el backend AUTO-CREA el registro en su base
 *      la primera vez que ve ese token (ver usuarios/firebase_auth.py).
 *   3. Se llama PATCH /usuarios/me/ con nombre + el rol elegido en este formulario.
 */
public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextInputLayout tilNombre, tilCorreo, tilPassword;
    private TextInputEditText etNombre, etCorreo, etPassword;
    private RadioGroup rgRol;
    private Button btnRegistrar;
    private TextView tvIrALogin;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
    }

    private void InitObjects() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.tilNombre = findViewById(R.id.tilNombre);
        this.tilCorreo = findViewById(R.id.tilCorreo);
        this.tilPassword = findViewById(R.id.tilPassword);
        this.etNombre = findViewById(R.id.etNombre);
        this.etCorreo = findViewById(R.id.etCorreo);
        this.etPassword = findViewById(R.id.etPassword);
        this.rgRol = findViewById(R.id.rgRol);
        this.btnRegistrar = findViewById(R.id.btnRegistrar);
        this.tvIrALogin = findViewById(R.id.tvIrALogin);
        this.progreso = findViewById(R.id.progreso);
    }

    private void InitListeners() {
        this.btnRegistrar.setOnClickListener(v -> intentarRegistrar());
        this.tvIrALogin.setOnClickListener(v -> finish());
    }

    private void intentarRegistrar() {
        tilNombre.setError(null);
        tilCorreo.setError(null);
        tilPassword.setError(null);

        String nombre = textoDe(etNombre).trim();
        String correo = textoDe(etCorreo).trim();
        String password = textoDe(etPassword);
        String rol = rgRol.getCheckedRadioButtonId() == R.id.rbDocente
                ? Usuario.ROL_DOCENTE : Usuario.ROL_ESTUDIANTE;

        boolean ok = true;
        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.error_nombre_vacio));
            ok = false;
        }
        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.error_correo_vacio));
            ok = false;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_corto));
            ok = false;
        }
        if (!ok) return;

        mostrarCargando(true);
        firebaseAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, tarea -> {
                    if (tarea.isSuccessful()) {
                        guardarNombreEnFirebase(nombre);
                        sincronizarConElApi(nombre, rol);
                    } else {
                        mostrarCargando(false);
                        tilCorreo.setError(getString(R.string.error_registro_fallido));
                    }
                });
    }

    private void guardarNombreEnFirebase(String nombre) {
        FirebaseUser usuario = firebaseAuth.getCurrentUser();
        if (usuario == null) return;
        UserProfileChangeRequest cambios = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();
        usuario.updateProfile(cambios); // no es crítico si falla; el nombre real vive en el API
    }

    private void sincronizarConElApi(String nombre, String rol) {
        // Paso 1: cualquier llamada autenticada auto-provisiona el Usuario en el backend.
        ApiClient.usuarios().obtenerMiPerfil().enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> resp) {
                if (!resp.isSuccessful()) {
                    onError();
                    return;
                }
                // Paso 2: ahora sí lo actualizamos con el nombre y el rol elegidos aquí.
                Map<String, Object> cambios = new HashMap<>();
                cambios.put("nombre", nombre);
                cambios.put("rol", rol);
                ApiClient.usuarios().actualizarMiPerfil(cambios).enqueue(new Callback<Usuario>() {
                    @Override
                    public void onResponse(Call<Usuario> call, Response<Usuario> resp) {
                        mostrarCargando(false);
                        if (resp.isSuccessful()) {
                            irAHome();
                        } else {
                            onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Usuario> call, Throwable t) {
                        onError();
                    }
                });
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                onError();
            }
        });
    }

    private void onError() {
        mostrarCargando(false);
        // La cuenta de Firebase ya quedó creada aunque esto falle; con volver a iniciar
        // sesión, obtenerMiPerfil() la sincroniza. Lo típico aquí es el API caído o mal
        // configurada la IP en ApiClient.BASE_URL — ver README.
        tilCorreo.setError(getString(R.string.error_sincronizar_api));
    }

    private void irAHome() {
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnRegistrar.setEnabled(!cargando);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
