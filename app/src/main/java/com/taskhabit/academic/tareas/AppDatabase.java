package com.taskhabit.academic.tareas;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Versión 3: se agregaron id_remoto, pendiente y eliminada para sincronizar con el backend.
@Database(entities = {Tarea.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instancia;

    public abstract TareaDao tareaDao();

    public static AppDatabase obtener(Context contexto) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            contexto.getApplicationContext(),
                            AppDatabase.class,
                            "taskhabit.db"
                    )
                            // Si cambia la tabla, en vez de escribir una migración a mano se
                            // recrea vacía. Ya no se pierde nada importante: las tareas
                            // sincronizadas se vuelven a bajar del servidor.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }
}
