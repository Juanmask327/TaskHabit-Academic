package com.taskhabit.academic.tareas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.taskhabit.academic.R;

import java.util.List;

/**
 * Lista de tareas y hábitos — offline-first (punto 2 del acta).
 * La lista se lee SIEMPRE de Room (funciona sin internet) y después se sincroniza
 * con el backend; si no hay conexión se muestra un aviso y los cambios esperan.
 */
public class TareasActivity extends AppCompatActivity implements TareaAdapter.Escuchador {

    private MaterialToolbar toolbar;
    private AppDatabase db;
    private TareaAdapter adaptador;
    private TextView tvVacio, tvEstadoSync;
    private RecyclerView rvTareas;
    private FloatingActionButton fabAgregar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tareas);
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
        cargarTareas();  // primero lo local (inmediato)...
        sincronizar();   // ...y luego se actualiza con el servidor
    }

    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.db = AppDatabase.obtener(this);
        this.rvTareas = findViewById(R.id.rvTareas);
        this.tvVacio = findViewById(R.id.tvVacio);
        this.tvEstadoSync = findViewById(R.id.tvEstadoSync);
        this.fabAgregar = findViewById(R.id.fabAgregar);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.fabAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, TareaFormActivity.class)));
    }

    private void configurarLista() {
        this.adaptador = new TareaAdapter(this);
        this.rvTareas.setLayoutManager(new LinearLayoutManager(this));
        this.rvTareas.setAdapter(this.adaptador);
    }

    private String uidActual() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void cargarTareas() {
        String uid = uidActual();
        DbExecutor.ejecutar(() -> {
            List<Tarea> tareas = db.tareaDao().obtenerTodas(uid);
            DbExecutor.enHiloPrincipal(() -> mostrarTareas(tareas));
        });
    }

    private void mostrarTareas(List<Tarea> tareas) {
        adaptador.setDatos(tareas);
        tvVacio.setVisibility(tareas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Sube los cambios pendientes, baja los del servidor y refresca la lista. */
    private void sincronizar() {
        SincronizadorTareas.sincronizar(db, uidActual(), exito -> {
            tvEstadoSync.setVisibility(exito ? View.GONE : View.VISIBLE);
            cargarTareas();
        });
    }

    @Override
    public void onCambiarCompletada(Tarea tarea, boolean completada) {
        tarea.setCompletada(completada);
        tarea.setPendiente(true); // hay un cambio que subir
        DbExecutor.ejecutar(() -> {
            db.tareaDao().actualizar(tarea);
            DbExecutor.enHiloPrincipal(this::sincronizar);
        });
    }

    @Override
    public void onEditar(Tarea tarea) {
        startActivity(new Intent(this, TareaFormActivity.class)
                .putExtra(TareaFormActivity.EXTRA_ID_TAREA, tarea.getId()));
    }

    @Override
    public void onEliminar(Tarea tarea) {
        DbExecutor.ejecutar(() -> {
            BorradoTareas.marcarOEliminar(db.tareaDao(), tarea);
            DbExecutor.enHiloPrincipal(() -> {
                cargarTareas();
                sincronizar();
            });
        });
    }
}
