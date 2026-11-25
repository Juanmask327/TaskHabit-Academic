package com.taskhabit.academic.tareas;

import android.util.Log;

import com.taskhabit.academic.auth.ApiClient;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * Sincroniza Room (celular) con PostgreSQL (backend) en dos pasos:
 *
 *   1. SUBIR: recorre las tareas con pendiente = true y manda POST (nueva), PUT (editada)
 *      o DELETE (borrada). Si el servidor confirma, se marca pendiente = false.
 *   2. BAJAR: pide GET /tareas y actualiza Room con lo que haya en el servidor
 *      (por ejemplo, tareas creadas desde otro celular con la misma cuenta).
 *
 * Regla de conflicto: si una tarea tiene cambios locales sin subir, gana el celular;
 * si no, gana el servidor.
 *
 * Todo corre en el hilo de DbExecutor (un solo hilo): así la sincronización nunca se
 * cruza con un guardado que haga la pantalla al mismo tiempo. Por eso aquí se usa
 * execute() (petición bloqueante) en vez de enqueue().
 */
public final class SincronizadorTareas {

    private static final String TAG = "SincronizadorTareas";

    /** Quien llama se entera en el hilo principal de si se pudo hablar con el servidor. */
    public interface Resultado {
        void onTerminado(boolean exito);
    }

    private SincronizadorTareas() { }

    public static void sincronizar(AppDatabase db, String usuarioUid, Resultado resultado) {
        DbExecutor.ejecutar(() -> {
            boolean exito = sincronizarAhora(db.tareaDao(), usuarioUid);
            DbExecutor.enHiloPrincipal(() -> resultado.onTerminado(exito));
        });
    }

    private static boolean sincronizarAhora(TareaDao dao, String usuarioUid) {
        TareaApi api = ApiClient.tareas();
        try {
            subirPendientes(dao, api, usuarioUid);
            bajarDelServidor(dao, api, usuarioUid);
            return true;
        } catch (IOException e) {
            // Sin internet o servidor apagado: los cambios siguen guardados en Room
            // con pendiente = true y se suben en la próxima sincronización.
            Log.w(TAG, "No se pudo sincronizar: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ Paso 1: subir

    private static void subirPendientes(TareaDao dao, TareaApi api, String usuarioUid) throws IOException {
        for (Tarea tarea : dao.obtenerPendientes(usuarioUid)) {
            if (tarea.isEliminada()) {
                subirEliminacion(dao, api, tarea);
            } else if (tarea.getIdRemoto() == null) {
                subirNueva(dao, api, tarea);
            } else {
                subirEdicion(dao, api, tarea);
            }
        }
    }

    private static void subirEliminacion(TareaDao dao, TareaApi api, Tarea tarea) throws IOException {
        if (tarea.getIdRemoto() != null) {
            Response<Void> resp = api.eliminar(tarea.getIdRemoto()).execute();
            // 404 = ya no existía en el servidor; el resultado es el mismo.
            if (!resp.isSuccessful() && resp.code() != 404) {
                throw new IOException("DELETE respondió " + resp.code());
            }
        }
        dao.eliminar(tarea); // ahora sí se borra la fila del celular
    }

    private static void subirNueva(TareaDao dao, TareaApi api, Tarea tarea) throws IOException {
        Response<TareaRemota> resp = api.crear(TareaRemota.desde(tarea)).execute();
        if (!resp.isSuccessful() || resp.body() == null) {
            registrarRechazo(tarea, resp.code());
            return;
        }
        tarea.setIdRemoto(resp.body().getId()); // desde ahora la fila local "conoce" su id en PostgreSQL
        tarea.setPendiente(false);
        dao.actualizar(tarea);
    }

    private static void subirEdicion(TareaDao dao, TareaApi api, Tarea tarea) throws IOException {
        Response<TareaRemota> resp = api.actualizar(tarea.getIdRemoto(), TareaRemota.desde(tarea)).execute();
        if (resp.code() == 404) {
            dao.eliminar(tarea); // la borraron en el servidor (desde otro dispositivo)
            return;
        }
        if (!resp.isSuccessful()) {
            registrarRechazo(tarea, resp.code());
            return;
        }
        tarea.setPendiente(false);
        dao.actualizar(tarea);
    }

    /**
     * Error 4xx/5xx que no es de conexión (ej. 400 por un dato inválido): se deja la tarea
     * pendiente y se sigue con las demás, para que una sola no bloquee la sincronización.
     */
    private static void registrarRechazo(Tarea tarea, int codigo) throws IOException {
        if (codigo == 401) {
            throw new IOException("Sesión no válida (401)");
        }
        Log.w(TAG, "El servidor rechazó la tarea local " + tarea.getId() + " con HTTP " + codigo);
    }

    // ------------------------------------------------------------------ Paso 2: bajar

    private static void bajarDelServidor(TareaDao dao, TareaApi api, String usuarioUid) throws IOException {
        Response<List<TareaRemota>> resp = api.listar().execute();
        if (!resp.isSuccessful() || resp.body() == null) {
            throw new IOException("GET /tareas respondió " + resp.code());
        }

        // Índice de las tareas locales que ya existen en el servidor: idRemoto -> tarea.
        Map<Long, Tarea> locales = new HashMap<>();
        for (Tarea t : dao.obtenerSincronizadas(usuarioUid)) {
            locales.put(t.getIdRemoto(), t);
        }

        for (TareaRemota remota : resp.body()) {
            Tarea local = locales.remove(remota.getId());
            if (local == null) {
                dao.insertar(nuevaDesdeServidor(remota, usuarioUid)); // creada en otro dispositivo
            } else if (!local.isPendiente()) {
                copiarDelServidor(remota, local);                     // gana el servidor
                dao.actualizar(local);
            }
            // Si local.isPendiente(): gana el celular; se subirá en la próxima sincronización.
        }

        // Lo que quedó en el mapa ya no existe en el servidor: se borró desde otro dispositivo.
        for (Tarea huerfana : locales.values()) {
            if (!huerfana.isPendiente()) {
                dao.eliminar(huerfana);
            }
        }
    }

    private static Tarea nuevaDesdeServidor(TareaRemota r, String usuarioUid) {
        Tarea t = new Tarea(usuarioUid, r.getTitulo(), r.getDescripcion(), r.getTipo(),
                r.getPrioridad(), r.isCompletada(), milisegundos(r.getFechaCreacion()));
        t.setIdRemoto(r.getId());
        t.setPendiente(false);
        return t;
    }

    private static void copiarDelServidor(TareaRemota r, Tarea t) {
        t.setTitulo(r.getTitulo());
        t.setDescripcion(r.getDescripcion());
        t.setTipo(r.getTipo());
        t.setPrioridad(r.getPrioridad());
        t.setCompletada(r.isCompletada());
    }

    /** "2026-09-27T18:29:08Z" -> milisegundos (el formato que guarda Room). */
    private static long milisegundos(String fechaIso) {
        try {
            return Instant.parse(fechaIso).toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
