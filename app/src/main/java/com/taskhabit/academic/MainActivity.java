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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taskhabit.academic.asistencia.AsistenciaRepository;
import com.taskhabit.academic.asistencia.ListaAsistenciaActivity;
import com.taskhabit.academic.asistencia.QrScannerActivity;
import com.taskhabit.academic.asistencia.RegistroAsistencia;
import com.taskhabit.academic.asistencia.Repositorios;
import com.taskhabit.academic.asistencia.SesionDocenteActivity;
import com.taskhabit.academic.auth.ApiClient;
import com.taskhabit.academic.auth.Usuario;
import com.taskhabit.academic.reportes.ReportesActivity;
import com.taskhabit.academic.tareas.TareasActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de inicio DESPUÉS de que hay sesión. Si no hay nadie logueado, manda a Login.
 * Una vez sabe el rol (viene del API, no de Firebase), muestra un menú distinto:
 *   - Docente: crear sesión/QR y ver la asistencia de sus sesiones.
 *   - Estudiante: escanear el QR para registrar su asistencia.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvBienvenida, tvRol, tvError;
    private ProgressBar progreso;
    private Button btnCrearSesion, btnVerAsistencia, btnEscanearQr, btnMisTareas, btnMisReportes,
            btnCerrarSesion;

    private Usuario miPerfil;

    // Captura lo que devuelve QrScannerActivity (ver procesarResultadoEscaner).
    // Se registra como campo porque Android exige hacerlo antes de que la Activity arranque.
    private final ActivityResultLauncher<Intent> lanzadorEscaner = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::procesarResultadoEscaner);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
    }

    private void InitObjects() {
        this.tvBienvenida = findViewById(R.id.tvBienvenida);
        this.tvRol = findViewById(R.id.tvRol);
        this.tvError = findViewById(R.id.tvError);
        this.progreso = findViewById(R.id.progreso);
        this.btnCrearSesion = findViewById(R.id.btnCrearSesion);
        this.btnVerAsistencia = findViewById(R.id.btnVerAsistencia);
        this.btnEscanearQr = findViewById(R.id.btnEscanearQr);
        this.btnMisTareas = findViewById(R.id.btnMisTareas);
        this.btnMisReportes = findViewById(R.id.btnMisReportes);
        this.btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }

    private void InitListeners() {
        this.btnCrearSesion.setOnClickListener(v ->
                startActivity(new Intent(this, SesionDocenteActivity.class)));
        this.btnVerAsistencia.setOnClickListener(v ->
                startActivity(new Intent(this, ListaAsistenciaActivity.class)));
        this.btnEscanearQr.setOnClickListener(v -> abrirEscaner());
        this.btnMisTareas.setOnClickListener(v ->
                startActivity(new Intent(this, TareasActivity.class)));
        this.btnMisReportes.setOnClickListener(v ->
                startActivity(new Intent(this, ReportesActivity.class)));
        this.btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void abrirEscaner() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String idEstudiante = fbUser != null ? fbUser.getUid() : "desconocido";
        String nombre = miPerfil != null ? miPerfil.getNombre() : idEstudiante;

        lanzadorEscaner.launch(new Intent(this, QrScannerActivity.class)
                .putExtra(QrScannerActivity.EXTRA_ID_ESTUDIANTE, idEstudiante)
                .putExtra(QrScannerActivity.EXTRA_NOMBRE_ESTUDIANTE, nombre));
    }

    /**
     * Guarda en el repositorio de asistencia lo que devolvió el escáner — sin esto,
     * el docente nunca vería la asistencia del estudiante.
     */
    private void procesarResultadoEscaner(ActivityResult resultado) {
        if (resultado.getResultCode() != RESULT_OK || resultado.getData() == null) return;
        String json = resultado.getData().getStringExtra(QrScannerActivity.EXTRA_REGISTRO_JSON);
        RegistroAsistencia registro = RegistroAsistencia.desdeJson(json);
        if (registro == null) return;

        Repositorios.asistencia(this).guardarRegistro(registro,
                new AsistenciaRepository.Resultado<Boolean>() {
                    @Override
                    public void onExito(Boolean esNuevo) {
                        if (!esNuevo) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    R.string.aviso_duplicado, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        irALogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            irALogin();
            return;
        }
        cargarMiPerfil();
    }

    private void cargarMiPerfil() {
        mostrarCargando(true);
        ApiClient.usuarios().obtenerMiPerfil().enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> resp) {
                mostrarCargando(false);
                if (resp.isSuccessful() && resp.body() != null) {
                    miPerfil = resp.body();
                    mostrarPerfil(miPerfil);
                } else {
                    mostrarError();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                mostrarCargando(false);
                mostrarError();
            }
        });
    }

    private void mostrarPerfil(Usuario usuario) {
        tvError.setVisibility(View.GONE);
        tvBienvenida.setText(getString(R.string.bienvenida, usuario.getNombre()));
        tvBienvenida.setVisibility(View.VISIBLE);

        boolean docente = usuario.esDocente();
        tvRol.setText(docente ? R.string.rol_docente : R.string.rol_estudiante);
        tvRol.setVisibility(View.VISIBLE);

        // El menú depende del rol que devolvió el API — Firebase por sí solo no lo sabe.
        btnCrearSesion.setVisibility(docente ? View.VISIBLE : View.GONE);
        btnVerAsistencia.setVisibility(docente ? View.VISIBLE : View.GONE);
        btnEscanearQr.setVisibility(docente ? View.GONE : View.VISIBLE);
    }

    private void mostrarError() {
        tvBienvenida.setVisibility(View.GONE);
        tvRol.setVisibility(View.GONE);
        btnCrearSesion.setVisibility(View.GONE);
        btnVerAsistencia.setVisibility(View.GONE);
        btnEscanearQr.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
    }

    private void irALogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
