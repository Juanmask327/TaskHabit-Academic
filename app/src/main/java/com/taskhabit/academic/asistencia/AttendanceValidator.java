package com.taskhabit.academic.asistencia;

import android.location.Location;
import android.os.Build;

/**
 * Regla de negocio de la asistencia: el QR debe estar vigente y el estudiante
 * debe encontrarse dentro del radio del salón. No depende de ninguna pantalla,
 * así que es fácil de reutilizar o probar.
 */
public final class AttendanceValidator {

    /**
     * Si es true, se rechazan ubicaciones enviadas por apps de "GPS falso".
     * Ponlo en false solo si necesitas probar con ubicación simulada.
     */
    public static final boolean RECHAZAR_UBICACION_SIMULADA = true;

    public enum Motivo { VALIDA, QR_VENCIDO, FUERA_DE_RADIO, UBICACION_SIMULADA }

    public static final class Resultado {
        public final Motivo motivo;
        public final float distanciaMetros;

        Resultado(Motivo motivo, float distanciaMetros) {
            this.motivo = motivo;
            this.distanciaMetros = distanciaMetros;
        }

        public boolean esValida() {
            return motivo == Motivo.VALIDA;
        }
    }

    private AttendanceValidator() { }

    public static Resultado validar(SesionClase sesion, Location ubicacionEstudiante, long ahoraMillis) {
        float[] distancia = new float[1];
        Location.distanceBetween(
                ubicacionEstudiante.getLatitude(), ubicacionEstudiante.getLongitude(),
                sesion.getLatitud(), sesion.getLongitud(),
                distancia);

        if (sesion.estaVencida(ahoraMillis)) {
            return new Resultado(Motivo.QR_VENCIDO, distancia[0]);
        }
        if (RECHAZAR_UBICACION_SIMULADA && esSimulada(ubicacionEstudiante)) {
            return new Resultado(Motivo.UBICACION_SIMULADA, distancia[0]);
        }
        if (distancia[0] > sesion.getRadioMetros()) {
            return new Resultado(Motivo.FUERA_DE_RADIO, distancia[0]);
        }
        return new Resultado(Motivo.VALIDA, distancia[0]);
    }

    @SuppressWarnings("deprecation")
    private static boolean esSimulada(Location ubicacion) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ubicacion.isMock();
        }
        return ubicacion.isFromMockProvider();
    }
}
