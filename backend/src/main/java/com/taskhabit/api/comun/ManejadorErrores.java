package com.taskhabit.api.comun;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejo centralizado de errores.
 *
 * @RestControllerAdvice "escucha" las excepciones que salen de CUALQUIER controlador.
 * Cada método @ExceptionHandler atrapa un tipo de excepción y decide el código HTTP.
 * Así los controladores y servicios solo lanzan excepciones y no se llenan de try/catch.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** 404: el recurso no existe o no pertenece al usuario. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    /** 403: autenticado pero sin permiso. */
    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ErrorRespuesta> accesoDenegado(AccesoDenegadoException e) {
        return construir(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    /** 400: reglas de negocio violadas (ej. fecha_fin antes de fecha_inicio). */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorRespuesta> argumentoInvalido(IllegalArgumentException e) {
        return construir(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    /**
     * 400: falló alguna anotación de validación (@NotBlank, @Size...) de un DTO marcado con @Valid.
     * Se devuelve un mapa campo -> mensaje para que la app pueda marcar el campo exacto.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError campo : e.getBindingResult().getFieldErrors()) {
            detalles.put(campo.getField(), campo.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
    }

    /** 400: el JSON está mal formado o trae un valor imposible (ej. "tipo": "OTRO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorRespuesta> jsonIlegible(HttpMessageNotReadableException e) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo JSON no es válido o tiene valores no permitidos", null);
    }

    /** 400: un parámetro de la URL tiene el tipo equivocado (ej. /tareas/abc en vez de /tareas/5). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorRespuesta> tipoParametro(MethodArgumentTypeMismatchException e) {
        return construir(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + e.getName(), null);
    }

    /**
     * 409: PostgreSQL rechazó la operación por una restricción (UNIQUE, CHECK, FOREIGN KEY).
     * Ej.: dos peticiones simultáneas intentan crear el mismo firebase_uid.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        return construir(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos", null);
    }

    private ResponseEntity<ErrorRespuesta> construir(HttpStatus estado, String mensaje, Map<String, String> detalles) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                estado.value(), estado.getReasonPhrase(), mensaje, detalles, Instant.now());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
