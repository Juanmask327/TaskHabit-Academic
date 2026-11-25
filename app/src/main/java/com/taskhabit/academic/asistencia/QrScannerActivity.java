package com.taskhabit.academic.asistencia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.taskhabit.academic.R;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ESTUDIANTE: escanea el QR del docente con la CÁMARA (CameraX + ML Kit), toma su
 * ubicación con el GPS y valida que el QR esté vigente y que esté dentro del radio.
 *
 * Contrato para integrarlo:
 *  - Entrada:  extra EXTRA_ID_ESTUDIANTE (String) con el id del estudiante (p. ej. uid de FirebaseAuth)
 *              y, opcional, EXTRA_NOMBRE_ESTUDIANTE (String) para mostrarlo en la lista del docente.
 *  - Salida:   RESULT_OK con el extra EXTRA_REGISTRO_JSON (ver RegistroAsistencia.desdeJson()).
 *              Si el usuario cierra sin escanear, el resultado es RESULT_CANCELED.
 */
public class QrScannerActivity extends AppCompatActivity {

    public static final String EXTRA_ID_ESTUDIANTE = "id_estudiante";
    public static final String EXTRA_NOMBRE_ESTUDIANTE = "nombre_estudiante";
    public static final String EXTRA_REGISTRO_JSON = "registro_json";

    private static final String[] PERMISOS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private enum Nivel { NEUTRO, OK, ERROR }

    private PreviewView previewView;
    private TextView tvEstado, tvDetalle;
    private Button btnPermisos, btnReintentar, btnListo;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean procesando = new AtomicBoolean(false);

    private ExecutorService ejecutorCamara;
    private BarcodeScanner escaner;
    private LocationHelper locationHelper;

    private boolean camaraActiva = false;
    private int intentosPermisos = 0;
    private String idEstudiante;
    private String nombreEstudiante;
    private RegistroAsistencia ultimoRegistro;

    private final ActivityResultLauncher<String[]> pedirPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            resultado -> {
                intentosPermisos++;
                intentarArrancar();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        InitObjects();
        InitListeners();
        loadExtras();
        solicitarPermisosSiFaltan();
    }

    private void InitObjects() {
        this.previewView = findViewById(R.id.previewView);
        this.tvEstado = findViewById(R.id.tvEstado);
        this.tvDetalle = findViewById(R.id.tvDetalle);
        this.btnPermisos = findViewById(R.id.btnPermisos);
        this.btnReintentar = findViewById(R.id.btnReintentar);
        this.btnListo = findViewById(R.id.btnListo);
        this.ejecutorCamara = Executors.newSingleThreadExecutor();
        this.escaner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build());
        this.locationHelper = new LocationHelper(this);
    }

    private void InitListeners() {
        this.btnPermisos.setOnClickListener(this::alPresionarPermisos);
        this.btnReintentar.setOnClickListener(v -> reanudarEscaneo());
        this.btnListo.setOnClickListener(v -> terminar());
    }

    /** Datos del estudiante que envía MainActivity. */
    private void loadExtras() {
        this.idEstudiante = getIntent().getStringExtra(EXTRA_ID_ESTUDIANTE);
        if (this.idEstudiante == null) this.idEstudiante = "desconocido";
        this.nombreEstudiante = getIntent().getStringExtra(EXTRA_NOMBRE_ESTUDIANTE);
        if (this.nombreEstudiante == null) this.nombreEstudiante = this.idEstudiante;
    }

    private void solicitarPermisosSiFaltan() {
        if (!permisosOk()) {
            pedirPermisos.launch(PERMISOS);
        }
    }

    private void alPresionarPermisos(View view) {
        if (intentosPermisos >= 2) {
            // Android dejó de mostrar el diálogo: hay que ir a los ajustes de la app.
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        } else {
            pedirPermisos.launch(PERMISOS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // También cubre el regreso desde los ajustes de la app.
        intentarArrancar();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        locationHelper.cancelar();
        escaner.close();
        ejecutorCamara.shutdown();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- permisos

    private boolean permisosOk() {
        boolean camara = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        return camara && LocationHelper.tienePermiso(this);
    }

    private void intentarArrancar() {
        if (permisosOk()) {
            btnPermisos.setVisibility(View.GONE);
            iniciarCamara();
        } else if (!camaraActiva) {
            mostrar(R.string.estado_permisos, getString(R.string.detalle_permisos), Nivel.ERROR);
            btnPermisos.setText(intentosPermisos >= 2 ? R.string.boton_ajustes : R.string.boton_permisos);
            btnPermisos.setVisibility(View.VISIBLE);
        }
    }

    // ------------------------------------------------------------------ cámara

    private void iniciarCamara() {
        if (camaraActiva) return;
        camaraActiva = true;
        mostrarApuntaAlQr();

        final ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                enlazarCamara(futuro.get());
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                camaraActiva = false;
                mostrar(R.string.estado_error, getString(R.string.error_camara), Nivel.ERROR);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void enlazarCamara(ProcessCameraProvider proveedor) {
        Preview vista = new Preview.Builder().build();
        vista.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analisis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analisis.setAnalyzer(ejecutorCamara, this::analizarImagen);

        proveedor.unbindAll();
        proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, vista, analisis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarImagen(@NonNull ImageProxy imageProxy) {
        Image imagen = imageProxy.getImage();
        if (imagen == null || procesando.get()) {
            imageProxy.close();
            return;
        }

        InputImage entrada = InputImage.fromMediaImage(
                imagen, imageProxy.getImageInfo().getRotationDegrees());

        escaner.process(entrada)
                .addOnSuccessListener(codigos -> {
                    for (Barcode codigo : codigos) {
                        String texto = codigo.getRawValue();
                        // compareAndSet: si varios frames leen el mismo QR, solo se procesa uno
                        if (texto != null && procesando.compareAndSet(false, true)) {
                            alLeerQr(texto);
                            break;
                        }
                    }
                })
                .addOnCompleteListener(tarea -> imageProxy.close());
    }

    // -------------------------------------------------------------- lógica QR+GPS

    private void alLeerQr(String texto) {
        final SesionClase sesion = SesionClase.desdeJson(texto);

        if (sesion == null) {
            mostrar(R.string.estado_qr_invalido, getString(R.string.detalle_qr_invalido), Nivel.ERROR);
            // Pausa de 2 s para no repetir el mensaje en cada frame y luego sigue escaneando
            handler.postDelayed(() -> {
                mostrarApuntaAlQr();
                procesando.set(false);
            }, 2000);
            return;
        }

        mostrar(R.string.estado_verificando,
                getString(R.string.detalle_verificando, sesion.getNombreClase()), Nivel.NEUTRO);

        locationHelper.obtenerUbicacionActual(new LocationHelper.Callback() {
            @Override
            public void onUbicacion(Location ubicacion) {
                completar(sesion, ubicacion);
            }

            @Override
            public void onError(String mensaje) {
                mostrar(R.string.estado_error, mensaje, Nivel.ERROR);
                mostrarBotonesResultado(false);
            }
        });
    }

    private void completar(SesionClase sesion, Location ubicacion) {
        long ahora = System.currentTimeMillis();
        AttendanceValidator.Resultado r = AttendanceValidator.validar(sesion, ubicacion, ahora);

        ultimoRegistro = new RegistroAsistencia(
                sesion.getIdSesion(), sesion.getNombreClase(), idEstudiante, nombreEstudiante, ahora,
                ubicacion.getLatitude(), ubicacion.getLongitude(),
                r.distanciaMetros, ubicacion.getAccuracy(),
                r.esValida(), r.motivo.name());

        String clase = sesion.getNombreClase();
        int distancia = Math.round(r.distanciaMetros);
        int radio = sesion.getRadioMetros();

        switch (r.motivo) {
            case VALIDA:
                mostrar(R.string.estado_valida,
                        getString(R.string.detalle_valida, clase, distancia, radio), Nivel.OK);
                break;
            case QR_VENCIDO:
                mostrar(R.string.estado_vencido,
                        getString(R.string.detalle_vencido, clase), Nivel.ERROR);
                break;
            case FUERA_DE_RADIO:
                mostrar(R.string.estado_fuera,
                        getString(R.string.detalle_fuera, clase, distancia, radio), Nivel.ERROR);
                break;
            case UBICACION_SIMULADA:
            default:
                mostrar(R.string.estado_simulada,
                        getString(R.string.detalle_simulada), Nivel.ERROR);
                break;
        }
        mostrarBotonesResultado(r.esValida());
    }

    // ------------------------------------------------------------------- UI

    private void mostrarApuntaAlQr() {
        mostrar(R.string.estado_apunta, getString(R.string.detalle_apunta), Nivel.NEUTRO);
    }

    private void mostrar(@StringRes int titulo, String detalle, Nivel nivel) {
        tvEstado.setText(titulo);
        tvDetalle.setText(detalle);

        int color;
        switch (nivel) {
            case OK:
                color = R.color.estado_ok;
                break;
            case ERROR:
                color = R.color.estado_error;
                break;
            default:
                color = R.color.blanco;
                break;
        }
        tvEstado.setTextColor(ContextCompat.getColor(this, color));
    }

    private void mostrarBotonesResultado(boolean valida) {
        btnReintentar.setVisibility(valida ? View.GONE : View.VISIBLE);
        btnListo.setText(valida ? R.string.boton_listo : R.string.boton_cerrar);
        btnListo.setVisibility(View.VISIBLE);
    }

    private void reanudarEscaneo() {
        ultimoRegistro = null;
        btnReintentar.setVisibility(View.GONE);
        btnListo.setVisibility(View.GONE);
        mostrarApuntaAlQr();
        procesando.set(false);
    }

    private void terminar() {
        if (ultimoRegistro != null) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REGISTRO_JSON, ultimoRegistro.toJson()));
        }
        finish();
    }
}
