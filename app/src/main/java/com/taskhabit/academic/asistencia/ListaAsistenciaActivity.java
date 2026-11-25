package com.taskhabit.academic.asistencia;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.taskhabit.academic.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DOCENTE: lista de estudiantes que registraron asistencia en una sesión.
 * Muestra también los intentos rechazados (fuera de radio, QR vencido...) para que el
 * docente tenga trazabilidad.
 *
 * Contrato: extra opcional EXTRA_ID_SESION; si no llega, se muestra la sesión más reciente.
 * Los datos salen de AsistenciaRepository, así que esta pantalla no cambia cuando exista el API.
 */
public class ListaAsistenciaActivity extends AppCompatActivity {

    public static final String EXTRA_ID_SESION = "id_sesion";

    private AsistenciaRepository repositorio;
    private MaterialToolbar toolbar;
    private Button btnActualizar;
    private TextInputLayout tilSesion;
    private MaterialAutoCompleteTextView acSesion;
    private TextView tvResumen, tvVacio;
    private RecyclerView rvRegistros;
    private RegistroAdapter adaptador;

    private List<SesionClase> sesiones = new ArrayList<>();
    private String idSesionActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_asistencia);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        loadExtras();
        configurarLista();
    }

    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.tilSesion = findViewById(R.id.tilSesion);
        this.acSesion = findViewById(R.id.acSesion);
        this.tvResumen = findViewById(R.id.tvResumen);
        this.tvVacio = findViewById(R.id.tvVacio);
        this.rvRegistros = findViewById(R.id.rvRegistros);
        this.btnActualizar = findViewById(R.id.btnActualizar);
        this.repositorio = Repositorios.asistencia(this);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.acSesion.setOnItemClickListener((padre, vista, posicion, id) -> {
            idSesionActual = sesiones.get(posicion).getIdSesion();
            cargarRegistros();
        });
        this.btnActualizar.setOnClickListener(v -> cargarSesiones());
    }

    /** Si venimos desde SesionDocenteActivity, llega la sesión que hay que mostrar. */
    private void loadExtras() {
        this.idSesionActual = getIntent().getStringExtra(EXTRA_ID_SESION);
    }

    private void configurarLista() {
        this.adaptador = new RegistroAdapter();
        this.rvRegistros.setLayoutManager(new LinearLayoutManager(this));
        this.rvRegistros.setAdapter(this.adaptador);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarSesiones(); // también refresca al volver de escanear un QR
    }

    private void cargarSesiones() {
        repositorio.listarSesiones(new AsistenciaRepository.Resultado<List<SesionClase>>() {
            @Override
            public void onExito(List<SesionClase> lista) {
                sesiones = lista;
                if (lista.isEmpty()) {
                    mostrarMensaje(getString(R.string.lista_sin_sesiones));
                    return;
                }

                // Por defecto la más reciente; si venía un id (o ya había una elegida), esa.
                int indice = 0;
                for (int i = 0; i < lista.size(); i++) {
                    if (lista.get(i).getIdSesion().equals(idSesionActual)) {
                        indice = i;
                        break;
                    }
                }
                idSesionActual = lista.get(indice).getIdSesion();

                List<String> etiquetas = new ArrayList<>();
                for (SesionClase s : lista) etiquetas.add(etiqueta(s));
                acSesion.setAdapter(new ArrayAdapter<>(ListaAsistenciaActivity.this,
                        android.R.layout.simple_dropdown_item_1line, etiquetas));
                acSesion.setText(etiquetas.get(indice), false);

                tilSesion.setVisibility(View.VISIBLE);
                cargarRegistros();
            }

            @Override
            public void onError(String mensaje) {
                mostrarMensaje(mensaje);
            }
        });
    }

    private void cargarRegistros() {
        repositorio.listarRegistros(idSesionActual,
                new AsistenciaRepository.Resultado<List<RegistroAsistencia>>() {
                    @Override
                    public void onExito(List<RegistroAsistencia> lista) {
                        int validas = 0;
                        for (RegistroAsistencia r : lista) {
                            if (r.isValida()) validas++;
                        }
                        tvResumen.setText(getString(R.string.lista_resumen, validas, lista.size() - validas));
                        tvResumen.setVisibility(View.VISIBLE);
                        adaptador.setDatos(lista);

                        if (lista.isEmpty()) {
                            tvVacio.setText(R.string.lista_sin_registros);
                            tvVacio.setVisibility(View.VISIBLE);
                            rvRegistros.setVisibility(View.GONE);
                        } else {
                            tvVacio.setVisibility(View.GONE);
                            rvRegistros.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String mensaje) {
                        mostrarMensaje(mensaje);
                    }
                });
    }

    /** Oculta selector y lista y deja solo un mensaje (sin sesiones, error de carga...). */
    private void mostrarMensaje(String mensaje) {
        tilSesion.setVisibility(View.GONE);
        tvResumen.setVisibility(View.GONE);
        rvRegistros.setVisibility(View.GONE);
        tvVacio.setText(mensaje);
        tvVacio.setVisibility(View.VISIBLE);
    }

    private String etiqueta(SesionClase sesion) {
        Date fecha = new Date(sesion.getCreadoEn());
        return getString(R.string.sesion_etiqueta,
                sesion.getNombreClase(),
                DateFormat.getDateFormat(this).format(fecha),
                DateFormat.getTimeFormat(this).format(fecha));
    }
}
