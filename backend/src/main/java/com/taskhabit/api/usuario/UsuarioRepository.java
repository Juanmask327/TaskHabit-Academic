package com.taskhabit.api.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * REPOSITORIO: la capa que habla con la base de datos.
 *
 * Es solo una INTERFAZ: Spring Data JPA crea la implementación al arrancar.
 * Al extender JpaRepository<Usuario, Long> (entidad, tipo de la llave) heredamos gratis:
 *   save(u)        -> INSERT si es nuevo (id null) o UPDATE si ya existe
 *   findById(id)   -> SELECT ... WHERE id = ?
 *   findAll()      -> SELECT * FROM usuarios
 *   deleteById(id) -> DELETE FROM usuarios WHERE id = ?
 *   count(), existsById(id), ...
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * "Consulta derivada": Spring lee el NOMBRE del método y genera el SQL.
     *   findBy + FirebaseUid  ->  SELECT * FROM usuarios WHERE firebase_uid = ?
     * Optional = puede que no exista; obliga a quien llama a manejar ese caso (sin null).
     */
    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    /** SELECT * FROM usuarios WHERE rol = ? AND activo = true ORDER BY nombre ASC */
    List<Usuario> findByRolAndActivoTrueOrderByNombreAsc(Rol rol);

    /** SELECT * FROM usuarios WHERE activo = true ORDER BY nombre ASC */
    List<Usuario> findByActivoTrueOrderByNombreAsc();
}
