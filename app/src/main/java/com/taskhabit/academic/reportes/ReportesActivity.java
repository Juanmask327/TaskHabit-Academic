package com.taskhabit.academic.reportes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.taskhabit.academic.R;
import com.taskhabit.academic.auth.ApiClient;

import java.util.List;

/**
 * Lista de reportes de desempeño (módulo Reportes del acta).
 * Los datos vienen del backend (GET /reportes); sin conexión se muestra un mensaje.
 */
public class ReportesActivity extends AppCompatActivity implements ReporteAdapter.Escuchador {

    private MaterialToolbar toolbar;
    private ReporteAdapter adaptador;
    private RecyclerView rvReportes;
    private TextView tvVacio;
    private ProgressBar progreso;
    private FloatingActionButton fabGenerar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reportes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        configurarLista();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarReportes(); // por si venimos de generar, editar o borrar uno
    }

    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.rvReportes = findViewById(R.id.rvReportes);
        this.tvVacio = findViewById(R.id.tvVacio);
        this.progreso = findViewById(R.id.progreso);
        this.fabGenerar = findViewById(R.id.fabGenerar);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.fabGenerar.setOnClickListener(v ->
                startActivity(new Intent(this, ReporteFormActivity.class)));
    }

    private void configurarLista() {
        this.adaptador = new ReporteAdapter(this);
        this.rvReportes.setLayoutManager(new LinearLayoutManager(this));
        this.rvReportes.setAdapter(this.adaptador);
    }

    private void cargarReportes() {
        progreso.setVisibility(View.VISIBLE);
        ApiClient.reportes().listar().enqueue(new LlamadaSimple<List<Reporte>>() {
            @Override
            void onExito(List<Reporte> reportes) {
                progreso.setVisibility(View.GONE);
                mostrarReportes(reportes);
            }

            @Override
            void onError() {
                progreso.setVisibility(View.GONE);
                mostrarMensaje(R.string.error_cargar_reportes);
            }
        });
    }

    private void mostrarReportes(List<Reporte> reportes) {
        adaptador.setDatos(reportes);
        if (reportes.isEmpty()) {
            mostrarMensaje(R.string.lista_reportes_vacia);
        } else {
            tvVacio.setVisibility(View.GONE);
        }
    }

    /** Usa el mismo TextView central para "no hay reportes" y para "sin conexión". */
    private void mostrarMensaje(int idTexto) {
        tvVacio.setText(idTexto);
        tvVacio.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEditar(Reporte reporte) {
        startActivity(new Intent(this, ReporteFormActivity.class)
                .putExtra(ReporteFormActivity.EXTRA_ID_REPORTE, reporte.getId()));
    }

    @Override
    public void onEliminar(Reporte reporte) {
        ApiClient.reportes().eliminar(reporte.getId()).enqueue(new LlamadaSimple<Void>() {
            @Override
            void onExito(Void cuerpo) {
                cargarReportes();
            }

            @Override
            void onError() {
                Snackbar.make(rvReportes, R.string.error_eliminar_reporte, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
