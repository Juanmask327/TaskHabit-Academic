package com.taskhabit.academic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextInputLayout tilCorreo, tilPassword;
    private TextInputEditText etCorreo, etPassword;
    private Button btnIniciar;
    private TextView tvIrARegistro;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
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
        this.tilCorreo = findViewById(R.id.tilCorreo);
        this.tilPassword = findViewById(R.id.tilPassword);
        this.etCorreo = findViewById(R.id.etCorreo);
        this.etPassword = findViewById(R.id.etPassword);
        this.btnIniciar = findViewById(R.id.btnIniciar);
        this.tvIrARegistro = findViewById(R.id.tvIrARegistro);
        this.progreso = findViewById(R.id.progreso);
    }

    private void InitListeners() {
        this.btnIniciar.setOnClickListener(v -> intentarIniciarSesion());
        this.tvIrARegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void intentarIniciarSesion() {
        tilCorreo.setError(null);
        tilPassword.setError(null);

        String correo = textoDe(etCorreo).trim();
        String password = textoDe(etPassword);

        boolean ok = true;
        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.error_correo_vacio));
            ok = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_vacio));
            ok = false;
        }
        if (!ok) return;

        mostrarCargando(true);
        firebaseAuth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, tarea -> {
                    mostrarCargando(false);
                    if (tarea.isSuccessful()) {
                        irAHome();
                    } else {
                        tilPassword.setError(getString(R.string.error_login_fallido));
                    }
                });
    }

    private void irAHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnIniciar.setEnabled(!cargando);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
