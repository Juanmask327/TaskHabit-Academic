package com.taskhabit.api.tarea;

/**
 * "Proyección": en vez de traer filas completas, una consulta puede devolver solo algunos valores.
 * Spring Data rellena esta interfaz con las columnas cuyo ALIAS coincide con el getter
 * (alias "total" -> getTotal(), alias "completadas" -> getCompletadas()).
 * La usa ReporteService para sacar estadísticas en UNA sola consulta.
 */
public interface ConteoTareas {

    long getTotal();

    long getCompletadas();
}
