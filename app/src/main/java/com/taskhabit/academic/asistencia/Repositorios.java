package com.taskhabit.academic.asistencia;

import android.content.Context;

/** Único lugar que decide qué fuente de datos usa la app. */
public final class Repositorios {

    private Repositorios() { }

    public static AsistenciaRepository asistencia(Context contexto) {
        // TODO (equipo): cuando exista el API REST, devolver new AsistenciaApiRepository(...)
        return new AsistenciaLocalRepository(contexto);
    }
}
