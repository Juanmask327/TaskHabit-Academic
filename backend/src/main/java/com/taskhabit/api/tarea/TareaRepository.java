package com.taskhabit.api.tarea;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskhabit.api.usuario.Usuario;

/**
 * Acceso a la tabla tareas. Hereda save/findById/delete... de JpaRepository.
 */
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    /**
     * Consulta derivada que además SEGURIZA: solo encuentra la tarea si es del usuario.
     *   SELECT * FROM tareas WHERE id = ? AND usuario_id = ?
     * Si la tarea existe pero es de otra persona, devuelve vacío -> respondemos 404
     * (no 403), para no revelar que ese id existe.
     */
    Optional<Tarea> findByIdAndUsuario(Long id, Usuario usuario);

    /**
     * Consulta JPQL escrita a mano con @Query.
     * JPQL se parece a SQL pero usa CLASES y ATRIBUTOS (Tarea, t.usuario), no tablas/columnas;
     * Hibernate la traduce al SQL de PostgreSQL.
     *
     * Filtros opcionales: si :tipo es null, la condición "(:tipo is null or ...)" es verdadera
     * y ese filtro se ignora. Así un solo método sirve para GET /tareas, ?tipo=HABITO,
     * ?completada=false o ambos.
     * @Param enlaza cada parámetro Java con su :nombre en la consulta.
     */
    @Query("""
            select t from Tarea t
            where t.usuario = :usuario
              and (:tipo is null or t.tipo = :tipo)
              and (:completada is null or t.completada = :completada)
            order by t.fechaCreacion desc
            """)
    List<Tarea> buscar(@Param("usuario") Usuario usuario,
                       @Param("tipo") TipoTarea tipo,
                       @Param("completada") Boolean completada);

    /**
     * Consulta de AGREGACIÓN para los reportes: cuenta en la base de datos (no en Java)
     * cuántas tareas de un tipo creó el usuario en un rango y cuántas completó.
     *   count(t)                          -> total de filas
     *   sum(case when completada ...)     -> suma 1 por cada fila completada
     *   coalesce(..., 0)                  -> si no hay filas, sum da NULL; lo cambiamos por 0
     * El rango es [desde, hasta): incluye desde y excluye hasta.
     */
    @Query("""
            select count(t) as total,
                   coalesce(sum(case when t.completada = true then 1 else 0 end), 0) as completadas
            from Tarea t
            where t.usuario = :usuario
              and t.tipo = :tipo
              and t.fechaCreacion >= :desde
              and t.fechaCreacion < :hasta
            """)
    ConteoTareas contar(@Param("usuario") Usuario usuario,
                        @Param("tipo") TipoTarea tipo,
                        @Param("desde") Instant desde,
                        @Param("hasta") Instant hasta);
}
