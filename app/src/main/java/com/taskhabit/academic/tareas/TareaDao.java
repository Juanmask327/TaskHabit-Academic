package com.taskhabit.academic.tareas;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface TareaDao {

    @Insert
    long insertar(Tarea tarea);

    @Update
    void actualizar(Tarea tarea);

    // Borrado FÍSICO: solo lo usa el sincronizador cuando el servidor ya confirmó.
    @Delete
    void eliminar(Tarea tarea);

    // Solo las tareas de ESE usuario y no borradas — pendientes primero, y entre ellas las más nuevas arriba.
    @Query("SELECT * FROM tareas WHERE usuario_uid = :usuarioUid AND eliminada = 0 " +
            "ORDER BY completada ASC, fecha_creacion DESC")
    List<Tarea> obtenerTodas(String usuarioUid);

    @Query("SELECT * FROM tareas WHERE id = :id LIMIT 1")
    Tarea obtenerPorId(long id);

    // --- Para SincronizadorTareas ---

    // Cambios locales que faltan por subir (nuevas, editadas o borradas).
    @Query("SELECT * FROM tareas WHERE usuario_uid = :usuarioUid AND pendiente = 1")
    List<Tarea> obtenerPendientes(String usuarioUid);

    // Todas las que ya existen en el servidor (tienen id_remoto), incluidas las borradas.
    @Query("SELECT * FROM tareas WHERE usuario_uid = :usuarioUid AND id_remoto IS NOT NULL")
    List<Tarea> obtenerSincronizadas(String usuarioUid);
}
