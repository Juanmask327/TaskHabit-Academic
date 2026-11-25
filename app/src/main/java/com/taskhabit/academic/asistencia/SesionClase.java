package com.taskhabit.academic.asistencia;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sesión de clase creada por el docente.
 * Es exactamente lo que viaja dentro del código QR, por eso el QR funciona sin conexión.
 */
public class SesionClase {

    private static final String TIPO = "taskhabit.asistencia";
    private static final int VERSION_QR = 1;

    private final String idSesion;
    private final String nombreClase;
    private final double latitud;
    private final double longitud;
    private final int radioMetros;
    private final long creadoEn;   // milisegundos desde epoch
    private final long venceEn;    // milisegundos desde epoch

    public SesionClase(String idSesion, String nombreClase, double latitud, double longitud,
                       int radioMetros, long creadoEn, long venceEn) {
        this.idSesion = idSesion;
        this.nombreClase = nombreClase;
        this.latitud = latitud;
        this.longitud = longitud;
        this.radioMetros = radioMetros;
        this.creadoEn = creadoEn;
        this.venceEn = venceEn;
    }

    public String getIdSesion() { return idSesion; }
    public String getNombreClase() { return nombreClase; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public int getRadioMetros() { return radioMetros; }
    public long getCreadoEn() { return creadoEn; }
    public long getVenceEn() { return venceEn; }

    public boolean estaVencida(long ahoraMillis) {
        return ahoraMillis > venceEn;
    }

    /** JSON compacto (claves cortas) para que el QR quede simple de escanear. */
    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", TIPO);
            o.put("v", VERSION_QR);
            o.put("id", idSesion);
            o.put("clase", nombreClase);
            o.put("lat", latitud);
            o.put("lng", longitud);
            o.put("radio", radioMetros);
            o.put("creado", creadoEn);
            o.put("vence", venceEn);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("No se pudo serializar la sesión", e);
        }
    }

    /** Devuelve null si el texto no es un QR de asistencia de TaskHabit (o está dañado). */
    public static SesionClase desdeJson(String texto) {
        if (texto == null) return null;
        try {
            JSONObject o = new JSONObject(texto);
            if (!TIPO.equals(o.optString("tipo"))) return null;
            if (o.optInt("v", -1) != VERSION_QR) return null;

            double lat = o.getDouble("lat");
            double lng = o.getDouble("lng");
            int radio = o.getInt("radio");
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180 || radio <= 0) return null;

            return new SesionClase(
                    o.getString("id"),
                    o.getString("clase"),
                    lat, lng, radio,
                    o.getLong("creado"),
                    o.getLong("vence"));
        } catch (JSONException e) {
            return null;
        }
    }
}
