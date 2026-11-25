package com.taskhabit.api.reporte;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskhabit.api.usuario.Usuario;

/** Acceso a la tabla reportes. */
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    /** SELECT * FROM reportes WHERE autor_id = ? ORDER BY fecha_generacion DESC */
    List<Reporte> findByAutorOrderByFechaGeneracionDesc(Usuario autor);

    /** SELECT * FROM reportes WHERE id = ? AND autor_id = ?  (solo si es mío) */
    Optional<Reporte> findByIdAndAutor(Long id, Usuario autor);
}
