package com.taskhabit.api.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskhabit.api.comun.RecursoNoEncontradoException;
import com.taskhabit.api.reporte.dto.ReportePeticion;
import com.taskhabit.api.reporte.dto.ReporteRespuesta;
import com.taskhabit.api.tarea.ConteoTareas;
import com.taskhabit.api.tarea.TareaRepository;
import com.taskhabit.api.tarea.TipoTarea;
import com.taskhabit.api.usuario.Usuario;
import com.taskhabit.api.usuario.UsuarioService;

/**
 * CRUD de reportes. Lo especial: al crear o actualizar, los totales se CALCULAN
 * consultando la tabla tareas (dos consultas de agregación, una por tipo).
 */
@Service
public class ReporteService {

    /** Zona horaria con la que se interpretan las fechas del periodo (la del servidor). */
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private final ReporteRepository reportes;
    private final TareaRepository tareas;
    private final UsuarioService usuarioService;

    public ReporteService(ReporteRepository reportes, TareaRepository tareas, UsuarioService usuarioService) {
        this.reportes = reportes;
        this.tareas = tareas;
        this.usuarioService = usuarioService;
    }

    /** CREATE: calcula las estadísticas y hace INSERT INTO reportes. */
    @Transactional
    public ReporteRespuesta generar(Jwt jwt, ReportePeticion peticion) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        Reporte reporte = new Reporte(yo);
        aplicarYCalcular(peticion, reporte, yo);
        return ReporteRespuesta.desde(reportes.save(reporte));
    }

    /** READ (lista): mis reportes, el más reciente primero. */
    @Transactional
    public List<ReporteRespuesta> listar(Jwt jwt) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        return reportes.findByAutorOrderByFechaGeneracionDesc(yo).stream()
                .map(ReporteRespuesta::desde)
                .toList();
    }

    /** READ (uno). */
    @Transactional
    public ReporteRespuesta obtener(Jwt jwt, Long id) {
        return ReporteRespuesta.desde(buscarPropio(jwt, id));
    }

    /** UPDATE: cambia título/periodo/observaciones y RECALCULA los totales (UPDATE reportes SET ...). */
    @Transactional
    public ReporteRespuesta actualizar(Jwt jwt, Long id, ReportePeticion peticion) {
        Reporte reporte = buscarPropio(jwt, id);
        aplicarYCalcular(peticion, reporte, reporte.getAutor());
        return ReporteRespuesta.desde(reporte); // dirty checking -> UPDATE al hacer COMMIT
    }

    /** DELETE: DELETE FROM reportes WHERE id = ? */
    @Transactional
    public void eliminar(Jwt jwt, Long id) {
        reportes.delete(buscarPropio(jwt, id));
    }

    private Reporte buscarPropio(Jwt jwt, Long id) {
        Usuario yo = usuarioService.usuarioActual(jwt);
        return reportes.findByIdAndAutor(id, yo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte", id));
    }

    /** Copia los datos de la petición y calcula los totales del periodo. */
    private void aplicarYCalcular(ReportePeticion p, Reporte r, Usuario autor) {
        // Regla de negocio: se valida aquí para dar un 400 claro. (La BD también la protege con un CHECK.)
        if (p.fechaFin().isBefore(p.fechaInicio())) {
            throw new IllegalArgumentException("fecha_fin no puede ser anterior a fecha_inicio");
        }
        r.setTitulo(p.titulo().trim());
        r.setFechaInicio(p.fechaInicio());
        r.setFechaFin(p.fechaFin());
        r.setObservaciones(p.observaciones());

        // El periodo [fecha_inicio 00:00, (fecha_fin + 1 día) 00:00) incluye el día fin completo.
        Instant desde = p.fechaInicio().atStartOfDay(ZONA).toInstant();
        Instant hasta = p.fechaFin().plusDays(1).atStartOfDay(ZONA).toInstant();

        // Dos consultas de agregación (COUNT + SUM) resueltas por PostgreSQL.
        ConteoTareas conteoTareas = tareas.contar(autor, TipoTarea.TAREA, desde, hasta);
        ConteoTareas conteoHabitos = tareas.contar(autor, TipoTarea.HABITO, desde, hasta);

        r.setTotalTareas((int) conteoTareas.getTotal());
        r.setTareasCompletadas((int) conteoTareas.getCompletadas());
        r.setTotalHabitos((int) conteoHabitos.getTotal());
        r.setHabitosCompletados((int) conteoHabitos.getCompletadas());
        r.setPorcentajeCumplimiento(porcentaje(
                conteoTareas.getCompletadas() + conteoHabitos.getCompletadas(),
                conteoTareas.getTotal() + conteoHabitos.getTotal()));
    }

    /** completadas / total * 100 redondeado a 2 decimales. Si no hay nada, 0 (evita dividir entre cero). */
    static BigDecimal porcentaje(long completadas, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(completadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
