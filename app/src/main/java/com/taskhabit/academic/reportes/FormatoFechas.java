package com.taskhabit.academic.reportes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Conversión de fechas entre los tres formatos que aparecen en reportes:
 *   - API:            "2026-09-22"            (LocalDate ISO, lo que espera el backend)
 *   - MaterialDatePicker: milisegundos UTC    (así entrega el calendario la selección)
 *   - Pantalla:       "22 sep 2026"
 */
final class FormatoFechas {

    private static final DateTimeFormatter PANTALLA =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"));

    private FormatoFechas() { }

    /** El calendario de Material entrega la medianoche UTC del día elegido. */
    static LocalDate desdeSelector(long milisUtc) {
        return Instant.ofEpochMilli(milisUtc).atZone(ZoneOffset.UTC).toLocalDate();
    }

    static long haciaSelector(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    static String paraPantalla(LocalDate fecha) {
        return PANTALLA.format(fecha);
    }

    /** "2026-09-22" -> "22 sep 2026" */
    static String paraPantalla(String fechaIso) {
        return paraPantalla(LocalDate.parse(fechaIso));
    }
}
