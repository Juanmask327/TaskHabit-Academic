package com.taskhabit.academic.asistencia;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Resultado que el módulo entrega al resto de la app (Room / API REST).
 * Es un POJO simple: para volverlo @Entity de Room o el cuerpo de un POST
 * basta con agregar las anotaciones o pasarlo por Gson/Retrofit.
 */
public class RegistroAsistencia {

    private final String idSesion;
    private final String nombreClase;
    private final String idEstudiante;     // más adelante: uid de FirebaseAuth
    private final String nombreEstudiante; // para mostrar en la lista (más adelante: displayName)
    private final long fechaHora;          // milisegundos desde epoch
    private final double latitud;
    private final double longitud;
    private final float distanciaMetros;   // distancia entre el estudiante y el salón
    private final float precisionMetros;   // precisión reportada por el GPS
    private final boolean valida;
    private final String motivo;           // VALIDA, QR_VENCIDO, FUERA_DE_RADIO, UBICACION_SIMULADA

    public RegistroAsistencia(String idSesion, String nombreClase,
                              String idEstudiante, String nombreEstudiante,
                              long fechaHora, double latitud, double longitud,
                              float distanciaMetros, float precisionMetros,
                              boolean valida, String motivo) {
        this.idSesion = idSesion;
        this.nombreClase = nombreClase;
        this.idEstudiante = idEstudiante;
        this.nombreEstudiante = nombreEstudiante;
        this.fechaHora = fechaHora;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaMetros = distanciaMetros;
        this.precisionMetros = precisionMetros;
        this.valida = valida;
        this.motivo = motivo;
    }

    public String getIdSesion() { return idSesion; }
    public String getNombreClase() { return nombreClase; }
    public String getIdEstudiante() { return idEstudiante; }
    public String getNombreEstudiante() { return nombreEstudiante; }
    public long getFechaHora() { return fechaHora; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public float getDistanciaMetros() { return distanciaMetros; }
    public float getPrecisionMetros() { return precisionMetros; }
    public boolean isValida() { return valida; }
    public String getMotivo() { return motivo; }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("idSesion", idSesion);
            o.put("clase", nombreClase);
            o.put("idEstudiante", idEstudiante);
            o.put("nombreEstudiante", nombreEstudiante);
            o.put("fechaHora", fechaHora);
            o.put("lat", redondear(latitud, 6));
            o.put("lng", redondear(longitud, 6));
            o.put("distanciaM", redondear(distanciaMetros, 1));
            o.put("precisionM", redondear(precisionMetros, 1));
            o.put("valida", valida);
            o.put("motivo", motivo);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("No se pudo serializar el registro", e);
        }
    }

    /** Devuelve null si el texto no es un registro válido. */
    public static RegistroAsistencia desdeJson(String texto) {
        if (texto == null) return null;
        try {
            JSONObject o = new JSONObject(texto);
            String idEstudiante = o.getString("idEstudiante");
            return new RegistroAsistencia(
                    o.getString("idSesion"),
                    o.getString("clase"),
                    idEstudiante,
                    o.optString("nombreEstudiante", idEstudiante),
                    o.getLong("fechaHora"),
                    o.getDouble("lat"),
                    o.getDouble("lng"),
                    (float) o.getDouble("distanciaM"),
                    (float) o.getDouble("precisionM"),
                    o.getBoolean("valida"),
                    o.getString("motivo"));
        } catch (JSONException e) {
            return null;
        }
    }

    private static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
