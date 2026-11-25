package com.taskhabit.academic.asistencia;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.WriterException;
import com.taskhabit.academic.R;

import java.util.Date;
import java.util.UUID;

/**
 * DOCENTE: define la clase, toma la ubicación del salón con el GPS y muestra un QR
 * que contiene la sesión (id, clase, coordenadas, radio y vencimiento).
 */
public class SesionDocenteActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilClase, tilRadio, tilVigencia;
    private TextInputEditText etClase, etRadio, etVigencia;
    private Button btnGenerar, btnVerAsistentes;
    private TextView tvEstado, tvInfo;
    private ImageView ivQr;

    private LocationHelper locationHelper;
    private AsistenciaRepository repositorio;
    private String idSesionActual;

    // Valores ya validados del formulario
    private String nombreClase;
    private int radioMetros;
    private int vigenciaMinutos;

    private final ActivityResultLauncher<String[]> pedirPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            resultado -> {
                if (LocationHelper.tienePermiso(this)) {
                    crearSesion();
                } else {
                    mostrarEstado(getString(R.string.error_sin_permiso_ubicacion));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sesion_docente);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        mantenerPantallaEncendida();
    }

    private void InitObjects() {
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.tilClase = findViewById(R.id.tilClase);
        this.tilRadio = findViewById(R.id.tilRadio);
        this.tilVigencia = findViewById(R.id.tilVigencia);
        this.etClase = findViewById(R.id.etClase);
        this.etRadio = findViewById(R.id.etRadio);
        this.etVigencia = findViewById(R.id.etVigencia);
        this.btnGenerar = findViewById(R.id.btnGenerar);
        this.btnVerAsistentes = findViewById(R.id.btnVerAsistentes);
        this.tvEstado = findViewById(R.id.tvEstado);
        this.tvInfo = findViewById(R.id.tvInfo);
        this.ivQr = findViewById(R.id.ivQr);
        this.locationHelper = new LocationHelper(this);
        this.repositorio = Repositorios.asistencia(this);
    }

    private void InitListeners() {
        this.toolbar.setNavigationOnClickListener(v -> finish());
        this.btnGenerar.setOnClickListener(v -> alPresionarGenerar());
        this.btnVerAsistentes.setOnClickListener(v -> startActivity(
                new Intent(this, ListaAsistenciaActivity.class)
                        .putExtra(ListaAsistenciaActivity.EXTRA_ID_SESION, idSesionActual)));
    }

    /** El docente suele proyectar el QR: evitamos que la pantalla se apague. */
    private void mantenerPantallaEncendida() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        locationHelper.cancelar();
        super.onDestroy();
    }

    private void alPresionarGenerar() {
        if (!leerYValidarCampos()) return;

        if (LocationHelper.tienePermiso(this)) {
            crearSesion();
        } else {
            pedirPermisos.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private boolean leerYValidarCampos() {
        tilClase.setError(null);
        tilRadio.setError(null);
        tilVigencia.setError(null);

        String nombre = textoDe(etClase).trim();
        Integer radio = enteroEnRango(textoDe(etRadio), 20, 1000);
        Integer vigencia = enteroEnRango(textoDe(etVigencia), 1, 240);

        boolean ok = true;
        if (nombre.isEmpty()) {
            tilClase.setError(getString(R.string.error_nombre_vacio_clase));
            ok = false;
        }
        if (radio == null) {
            tilRadio.setError(getString(R.string.error_radio));
            ok = false;
        }
        if (vigencia == null) {
            tilVigencia.setError(getString(R.string.error_vigencia));
            ok = false;
        }
        if (ok) {
            nombreClase = nombre;
            radioMetros = radio;
            vigenciaMinutos = vigencia;
        }
        return ok;
    }

    private void crearSesion() {
        btnGenerar.setEnabled(false);
        ivQr.setVisibility(View.GONE);
        tvInfo.setVisibility(View.GONE);
        btnVerAsistentes.setVisibility(View.GONE);
        mostrarEstado(getString(R.string.estado_obteniendo_ubicacion));

        locationHelper.obtenerUbicacionActual(new LocationHelper.Callback() {
            @Override
            public void onUbicacion(Location ubicacion) {
                btnGenerar.setEnabled(true);
                mostrarSesion(ubicacion);
            }

            @Override
            public void onError(String mensaje) {
                btnGenerar.setEnabled(true);
                mostrarEstado(mensaje);
            }
        });
    }

    private void mostrarSesion(Location ubicacion) {
        long ahora = System.currentTimeMillis();
        SesionClase sesion = new SesionClase(
                UUID.randomUUID().toString(),
                nombreClase,
                ubicacion.getLatitude(),
                ubicacion.getLongitude(),
                radioMetros,
                ahora,
                ahora + vigenciaMinutos * 60_000L);

        // Se guarda por el repositorio: hoy local (demo); con el API, se envía al backend.
        repositorio.guardarSesion(sesion, ok -> { });
        idSesionActual = sesion.getIdSesion();

        try {
            Bitmap qr = QrGenerator.generar(sesion.toJson(), 800);
            ivQr.setImageBitmap(qr);
            ivQr.setContentDescription(getString(R.string.descripcion_qr, nombreClase));
            ivQr.setVisibility(View.VISIBLE);

            String hasta = DateFormat.getTimeFormat(this).format(new Date(sesion.getVenceEn()));
            tvInfo.setText(getString(R.string.info_sesion,
                    nombreClase, hasta, radioMetros, Math.round(ubicacion.getAccuracy())));
            tvInfo.setVisibility(View.VISIBLE);
            btnVerAsistentes.setVisibility(View.VISIBLE);
            mostrarEstado(getString(R.string.estado_qr_listo));
        } catch (WriterException e) {
            mostrarEstado(getString(R.string.error_qr));
        }
    }

    private void mostrarEstado(String mensaje) {
        tvEstado.setText(mensaje);
        tvEstado.setVisibility(View.VISIBLE);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    /** Devuelve el número si está en [min, max]; si no, null. */
    private static Integer enteroEnRango(String texto, int min, int max) {
        try {
            int valor = Integer.parseInt(texto.trim());
            return (valor >= min && valor <= max) ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
