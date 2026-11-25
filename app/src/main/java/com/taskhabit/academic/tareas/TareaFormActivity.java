package com.taskhabit.academic.tareas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.taskhabit.academic.R;

/**
 * Sirve para crear una tarea/hábito nuevo (sin extras) o editar uno existente
 * (con EXTRA_ID_TAREA). Es la misma pantalla en los dos casos.
 * Solo guarda en Room y deja la tarea "pendiente": la lista (TareasActivity)
 * la sube al servidor al volver.
 */
public class TareaFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID_TAREA = "id_tarea";
    private static final long SIN_ID = -1L;

    private MaterialToolbar toolbar;
    private AppDatabase db;
    private long idTarea = SIN_ID;
    private Tarea tareaExistente; // no nulo solo en modo edición

    private TextInputLayout tilTitulo;
    private TextInputEditText etTitulo, etDescripcion;
    private RadioGroup rgTipo, rgPrioridad;
    private Button btnGuardar, btnEliminar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tarea_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        configurarModo();
    }

    /** setSupportActionBar va antes de configurarModo() para que setTitle(...) aparezca en la barra. */
    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.db = AppDatabase.obtener(this);
        this.tilTitulo = findViewById(R.id.tilTitulo);
        this.etTitulo = findViewById(R.id.etTitulo);
        this.etDescripcion = findViewById(R.id.etDescripcion);
        this.rgTipo = findViewById(R.id.rgTipo);
        this.rgPrioridad = findViewById(R.id.rgPrioridad);
        this.btnGuardar = findViewById(R.id.btnGuardar);
        this.btnEliminar = findViewById(R.id.btnEliminar);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.btnGuardar.setOnClickListener(v -> guardar());
        this.btnEliminar.setOnClickListener(v -> eliminar());
    }

    /** Decide si la pantalla es para crear o para editar, según el extra recibido. */
    private void configurarModo() {
        idTarea = getIntent().getLongExtra(EXTRA_ID_TAREA, SIN_ID);
        boolean esEdicion = idTarea != SIN_ID;
        setTitle(esEdicion ? R.string.form_titulo_editar : R.string.form_titulo_nueva);
        btnEliminar.setVisibility(esEdicion ? View.VISIBLE : View.GONE);
        if (esEdicion) {
            cargarParaEditar();
        }
    }

    private void cargarParaEditar() {
        DbExecutor.ejecutar(() -> {
            Tarea tarea = db.tareaDao().obtenerPorId(idTarea);
            DbExecutor.enHiloPrincipal(() -> mostrarParaEditar(tarea));
        });
    }

    private void mostrarParaEditar(Tarea tarea) {
        if (tarea == null) {
            finish(); // la borraron desde otro lado; no hay nada que editar
            return;
        }
        tareaExistente = tarea;
        etTitulo.setText(tarea.getTitulo());
        etDescripcion.setText(tarea.getDescripcion());
        rgTipo.check(tarea.esHabito() ? R.id.rbHabito : R.id.rbTarea);
        rgPrioridad.check(idPrioridad(tarea.getPrioridad()));
    }

    private void guardar() {
        tilTitulo.setError(null);
        String titulo = textoDe(etTitulo).trim();
        if (titulo.isEmpty()) {
            tilTitulo.setError(getString(R.string.error_titulo_vacio));
            return;
        }
        String descripcion = textoDe(etDescripcion).trim();
        String tipo = rgTipo.getCheckedRadioButtonId() == R.id.rbHabito
                ? Tarea.TIPO_HABITO : Tarea.TIPO_TAREA;
        String prioridad = prioridadDe(rgPrioridad.getCheckedRadioButtonId());

        if (tareaExistente != null) {
            actualizarExistente(titulo, descripcion, tipo, prioridad);
        } else {
            insertarNueva(titulo, descripcion, tipo, prioridad);
        }
    }

    private void actualizarExistente(String titulo, String descripcion, String tipo, String prioridad) {
        tareaExistente.setTitulo(titulo);
        tareaExistente.setDescripcion(descripcion);
        tareaExistente.setTipo(tipo);
        tareaExistente.setPrioridad(prioridad);
        tareaExistente.setPendiente(true); // hay cambios que subir al servidor
        DbExecutor.ejecutar(() -> {
            db.tareaDao().actualizar(tareaExistente);
            DbExecutor.enHiloPrincipal(this::finish);
        });
    }

    private void insertarNueva(String titulo, String descripcion, String tipo, String prioridad) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Nace con pendiente = true (valor por defecto en Tarea).
        Tarea nueva = new Tarea(uid, titulo, descripcion, tipo, prioridad,
                false, System.currentTimeMillis());
        DbExecutor.ejecutar(() -> {
            db.tareaDao().insertar(nueva);
            DbExecutor.enHiloPrincipal(this::finish);
        });
    }

    private void eliminar() {
        if (tareaExistente == null) return;
        DbExecutor.ejecutar(() -> {
            BorradoTareas.marcarOEliminar(db.tareaDao(), tareaExistente);
            DbExecutor.enHiloPrincipal(this::finish);
        });
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    private int idPrioridad(String prioridad) {
        if (Tarea.PRIORIDAD_ALTA.equals(prioridad)) return R.id.rbAlta;
        if (Tarea.PRIORIDAD_BAJA.equals(prioridad)) return R.id.rbBaja;
        return R.id.rbMedia;
    }

    private String prioridadDe(int idBotonMarcado) {
        if (idBotonMarcado == R.id.rbAlta) return Tarea.PRIORIDAD_ALTA;
        if (idBotonMarcado == R.id.rbBaja) return Tarea.PRIORIDAD_BAJA;
        return Tarea.PRIORIDAD_MEDIA;
    }
}
