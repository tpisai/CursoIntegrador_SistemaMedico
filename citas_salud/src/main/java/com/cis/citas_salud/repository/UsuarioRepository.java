package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByDni(String dni);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findByRol_NombreAndEstado(String rol, String estado);
}
