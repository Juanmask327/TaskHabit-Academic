package com.taskhabit.academic.reportes;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskhabit.academic.R;
import com.taskhabit.academic.auth.ApiClient;

import java.time.LocalDate;

import retrofit2.Call;

/**
 * Genera un reporte nuevo (sin extras) o edita uno existente (con EXTRA_ID_REPORTE).
 * El usuario elige título, periodo y observaciones; el servidor calcula los totales.
 */
public class ReporteFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID_REPORTE = "id_reporte";
    private static final long SIN_ID = -1L;
    private static final int DIAS_POR_DEFECTO = 7;

    private MaterialToolbar toolbar;
    private long idReporte = SIN_ID;
    private LocalDate fechaInicio, fechaFin;

    private TextInputLayout tilTitulo;
    private TextInputEditText etTitulo, etPeriodo, etObservaciones;
    private Button btnGuardar, btnEliminar;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reporte_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        configurarModo();
    }

    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.tilTitulo = findViewById(R.id.tilTitulo);
        this.etTitulo = findViewById(R.id.etTitulo);
        this.etPeriodo = findViewById(R.id.etPeriodo);
        this.etObservaciones = findViewById(R.id.etObservaciones);
        this.btnGuardar = findViewById(R.id.btnGuardar);
        this.btnEliminar = findViewById(R.id.btnEliminar);
        this.progreso = findViewById(R.id.progreso);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.etPeriodo.setOnClickListener(v -> abrirCalendario());
        this.btnGuardar.setOnClickListener(v -> guardar());
        this.btnEliminar.setOnClickListener(v -> eliminar());
    }

    /** Crear: periodo por defecto = últimos 7 días. Editar: se trae el reporte del servidor. */
    private void configurarModo() {
        idReporte = getIntent().getLongExtra(EXTRA_ID_REPORTE, SIN_ID);
        boolean esEdicion = idReporte != SIN_ID;
        setTitle(esEdicion ? R.string.form_reporte_editar : R.string.form_reporte_nuevo);
        btnEliminar.setVisibility(esEdicion ? View.VISIBLE : View.GONE);
        if (esEdicion) {
            cargarParaEditar();
        } else {
            LocalDate hoy = LocalDate.now();
            ponerPeriodo(hoy.minusDays(DIAS_POR_DEFECTO - 1), hoy);
        }
    }

    private void cargarParaEditar() {
        mostrarCargando(true);
        ApiClient.reportes().obtener(idReporte).enqueue(new LlamadaSimple<Reporte>() {
            @Override
            void onExito(Reporte reporte) {
                mostrarCargando(false);
                mostrarParaEditar(reporte);
            }

            @Override
            void onError() {
                mostrarCargando(false);
                mostrarError(R.string.error_cargar_reportes);
            }
        });
    }

    private void mostrarParaEditar(Reporte reporte) {
        etTitulo.setText(reporte.getTitulo());
        etObservaciones.setText(reporte.getObservaciones());
        ponerPeriodo(LocalDate.parse(reporte.getFechaInicio()), LocalDate.parse(reporte.getFechaFin()));
    }

    private void ponerPeriodo(LocalDate inicio, LocalDate fin) {
        fechaInicio = inicio;
        fechaFin = fin;
        etPeriodo.setText(getString(R.string.reporte_periodo,
                FormatoFechas.paraPantalla(inicio), FormatoFechas.paraPantalla(fin)));
    }

    /** Calendario de Material 3 para elegir un rango (inicio y fin) en un solo paso. */
    private void abrirCalendario() {
        MaterialDatePicker<Pair<Long, Long>> selector = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.hint_periodo)
                .setSelection(new Pair<>(
                        FormatoFechas.haciaSelector(fechaInicio),
                        FormatoFechas.haciaSelector(fechaFin)))
                .build();
        selector.addOnPositiveButtonClickListener(rango -> ponerPeriodo(
                FormatoFechas.desdeSelector(rango.first),
                FormatoFechas.desdeSelector(rango.second)));
        selector.show(getSupportFragmentManager(), "selector_periodo");
    }

    private void guardar() {
        tilTitulo.setError(null);
        String titulo = textoDe(etTitulo).trim();
        if (titulo.isEmpty()) {
            tilTitulo.setError(getString(R.string.error_titulo_vacio));
            return;
        }
        ReportePeticion peticion = new ReportePeticion(titulo,
                fechaInicio.toString(), fechaFin.toString(), textoDe(etObservaciones).trim());

        Call<Reporte> llamada = idReporte == SIN_ID
                ? ApiClient.reportes().generar(peticion)
                : ApiClient.reportes().actualizar(idReporte, peticion);
        enviar(llamada);
    }

    private void enviar(Call<Reporte> llamada) {
        mostrarCargando(true);
        llamada.enqueue(new LlamadaSimple<Reporte>() {
            @Override
            void onExito(Reporte reporte) {
                finish(); // la lista se recarga en su onResume
            }

            @Override
            void onError() {
                mostrarCargando(false);
                mostrarError(R.string.error_guardar_reporte);
            }
        });
    }

    private void eliminar() {
        mostrarCargando(true);
        ApiClient.reportes().eliminar(idReporte).enqueue(new LlamadaSimple<Void>() {
            @Override
            void onExito(Void cuerpo) {
                finish();
            }

            @Override
            void onError() {
                mostrarCargando(false);
                mostrarError(R.string.error_eliminar_reporte);
            }
        });
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!cargando);
        btnEliminar.setEnabled(!cargando);
    }

    private void mostrarError(int idTexto) {
        Snackbar.make(btnGuardar, idTexto, Snackbar.LENGTH_LONG).show();
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
