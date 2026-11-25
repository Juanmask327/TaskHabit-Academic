package com.taskhabit.academic.tareas;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room prohíbe consultar la base de datos en el hilo principal. Este es el patrón
 * más simple para Java sin agregar LiveData ni coroutines: correr en un hilo de
 * fondo y devolver el resultado al hilo principal para actualizar la pantalla.
 */
public final class DbExecutor {

    private static final ExecutorService FONDO = Executors.newSingleThreadExecutor();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private DbExecutor() { }

    public static void ejecutar(Runnable tarea) {
        FONDO.execute(tarea);
    }

    public static void enHiloPrincipal(Runnable accion) {
        PRINCIPAL.post(accion);
    }
}
