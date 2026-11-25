package com.taskhabit.academic.asistencia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.taskhabit.academic.R;

/**
 * Recurso del dispositivo #2: GPS.
 * Envuelve FusedLocationProviderClient para pedir UNA ubicación precisa y actual.
 * Los permisos en tiempo de ejecución los pide cada Activity; aquí solo se verifican.
 */
public class LocationHelper {

    public interface Callback {
        void onUbicacion(Location ubicacion);
        void onError(String mensaje);
    }

    private static final long TIEMPO_MAXIMO_MS = 15_000L;

    private final Context contexto;
    private final FusedLocationProviderClient cliente;
    private CancellationTokenSource cancelacion;

    public LocationHelper(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.cliente = LocationServices.getFusedLocationProviderClient(contexto);
    }

    /** true si el usuario concedió ubicación PRECISA (en Android 12+ puede dar solo aproximada). */
    public static boolean tienePermiso(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean gpsActivado(Context contexto) {
        LocationManager lm = (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
        return lm != null && LocationManagerCompat.isLocationEnabled(lm);
    }

    @SuppressLint("MissingPermission") // se verifica arriba con tienePermiso()
    public void obtenerUbicacionActual(final Callback callback) {
        if (!tienePermiso(contexto)) {
            callback.onError(contexto.getString(R.string.error_sin_permiso_ubicacion));
            return;
        }
        if (!gpsActivado(contexto)) {
            callback.onError(contexto.getString(R.string.error_gps_apagado));
            return;
        }

        CurrentLocationRequest peticion = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(TIEMPO_MAXIMO_MS)
                .build();

        cancelar();
        cancelacion = new CancellationTokenSource();
        cliente.getCurrentLocation(peticion, cancelacion.getToken())
                .addOnSuccessListener(ubicacion -> {
                    if (ubicacion != null) {
                        callback.onUbicacion(ubicacion);
                    } else {
                        callback.onError(contexto.getString(R.string.error_sin_senal_gps));
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError(contexto.getString(R.string.error_ubicacion_fallo)));
    }

    /** Llamar en onDestroy() para no dejar la petición viva. */
    public void cancelar() {
        if (cancelacion != null) {
            cancelacion.cancel();
            cancelacion = null;
        }
    }
}
