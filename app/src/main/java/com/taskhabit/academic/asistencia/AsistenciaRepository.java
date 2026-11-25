package com.taskhabit.academic.asistencia;

import java.util.List;

/**
 * Fuente de datos de la asistencia. Las pantallas SOLO hablan con esta interfaz.
 *
 * Hoy la implementa AsistenciaLocalRepository (SharedPreferences, solo para pruebas y demo).
 * Cuando exista el API de Spring Boot se crea AsistenciaApiRepository con Retrofit y se cambia
 * UNA línea en Repositorios.asistencia(). Por eso todos los métodos son asíncronos (callback):
 * una llamada de red también lo será.
 *
 * Endpoints propuestos para el backend:
 *   POST /api/asistencias                     cuerpo = RegistroAsistencia.toJson()   -> guardarRegistro
 *   GET  /api/sesiones/{idSesion}/asistencias                                       -> listarRegistros
 *   POST /api/sesiones                        cuerpo = SesionClase.toJson()          -> guardarSesion
 *   GET  /api/sesiones                        (las del docente autenticado)          -> listarSesiones
 * El backend debe rechazar duplicados con una restricción única (idSesion + idEstudiante) para
 * registros válidos, y responder 409 en ese caso.
 */
public interface AsistenciaRepository {

    interface Resultado<T> {
        void onExito(T datos);

        default void onError(String mensaje) { }
    }

    /** Guarda la sesión creada por el docente. */
    void guardarSesion(SesionClase sesion, Resultado<Void> resultado);

    /** Sesiones del docente, la más reciente primero. */
    void listarSesiones(Resultado<List<SesionClase>> resultado);

    /**
     * Guarda el registro de un estudiante.
     * Entrega true si se guardó como nuevo, o false si ya existía una asistencia VÁLIDA de ese
     * estudiante en esa sesión (duplicado: no se guarda otra vez).
     */
    void guardarRegistro(RegistroAsistencia registro, Resultado<Boolean> resultado);

    /** Registros de una sesión (válidos y rechazados), el más reciente primero. */
    void listarRegistros(String idSesion, Resultado<List<RegistroAsistencia>> resultado);
}
