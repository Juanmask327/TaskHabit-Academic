package com.taskhabit.api.comun;

import java.time.Instant;
import java.util.Map;

/**
 * Formato único de los errores del API, para que la app siempre reciba el mismo JSON:
 * { "estado": 404, "error": "Not Found", "mensaje": "...", "detalles": {...}, "fecha": "..." }
 *
 * "record" (Java 16+) = clase inmutable que solo guarda datos; Java genera solo el
 * constructor, los getters, equals, hashCode y toString.
 */
public record ErrorRespuesta(
        int estado,
        String error,
        String mensaje,
        Map<String, String> detalles, // errores por campo (validación); null si no aplica
        Instant fecha
) {
}
