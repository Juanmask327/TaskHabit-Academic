package com.taskhabit.academic.asistencia;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SOLO PARA PRUEBAS: guarda todo en SharedPreferences de este celular.
 * Sirve para demostrar la lista del docente sin backend; por eso solo ve lo registrado
 * en el mismo dispositivo. Se reemplaza por una implementación que use el API REST.
 */
public class AsistenciaLocalRepository implements AsistenciaRepository {

    private static final String PREFS = "taskhabit_asistencia_demo";
    private static final String CLAVE_SESIONES = "sesiones";
    private static final String CLAVE_REGISTROS = "registros";

    private final SharedPreferences prefs;

    public AsistenciaLocalRepository(Context contexto) {
        prefs = contexto.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void guardarSesion(SesionClase sesion, Resultado<Void> resultado) {
        List<String> lista = leer(CLAVE_SESIONES);
        lista.add(0, sesion.toJson());   // la más reciente primero
        escribir(CLAVE_SESIONES, lista);
        resultado.onExito(null);
    }

    @Override
    public void listarSesiones(Resultado<List<SesionClase>> resultado) {
        List<SesionClase> sesiones = new ArrayList<>();
        for (String texto : leer(CLAVE_SESIONES)) {
            SesionClase s = SesionClase.desdeJson(texto);
            if (s != null) sesiones.add(s);
        }
        resultado.onExito(sesiones);
    }

    @Override
    public void guardarRegistro(RegistroAsistencia registro, Resultado<Boolean> resultado) {
        List<String> lista = leer(CLAVE_REGISTROS);

        // Regla anti-duplicados: una sola asistencia VÁLIDA por estudiante y sesión.
        if (registro.isValida()) {
            for (String texto : lista) {
                RegistroAsistencia previo = RegistroAsistencia.desdeJson(texto);
                if (previo != null && previo.isValida()
                        && previo.getIdSesion().equals(registro.getIdSesion())
                        && previo.getIdEstudiante().equals(registro.getIdEstudiante())) {
                    resultado.onExito(false);
                    return;
                }
            }
        }

        lista.add(0, registro.toJson());
        escribir(CLAVE_REGISTROS, lista);
        resultado.onExito(true);
    }

    @Override
    public void listarRegistros(String idSesion, Resultado<List<RegistroAsistencia>> resultado) {
        List<RegistroAsistencia> registros = new ArrayList<>();
        for (String texto : leer(CLAVE_REGISTROS)) {
            RegistroAsistencia r = RegistroAsistencia.desdeJson(texto);
            if (r != null && r.getIdSesion().equals(idSesion)) registros.add(r);
        }
        resultado.onExito(registros);
    }

    /** Utilidad de pruebas: borra sesiones y registros guardados en este celular. */
    public void borrarTodo() {
        prefs.edit().clear().apply();
    }

    // Cada elemento se guarda como su propio JSON (el mismo que ya usan SesionClase y RegistroAsistencia).
    private List<String> leer(String clave) {
        List<String> salida = new ArrayList<>();
        String json = prefs.getString(clave, null);
        if (json == null) return salida;
        try {
            JSONArray arreglo = new JSONArray(json);
            for (int i = 0; i < arreglo.length(); i++) {
                salida.add(arreglo.getString(i));
            }
        } catch (JSONException e) {
            // Datos dañados: se ignoran y se empieza de cero.
        }
        return salida;
    }

    private void escribir(String clave, List<String> elementos) {
        JSONArray arreglo = new JSONArray();
        for (String e : elementos) arreglo.put(e);
        prefs.edit().putString(clave, arreglo.toString()).apply();
    }
}
