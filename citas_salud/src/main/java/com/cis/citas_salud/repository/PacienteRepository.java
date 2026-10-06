package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {

    Optional<Paciente> findByUsuario_IdUsuario(Integer idUsuario);

    /** Usuarios de los pacientes activos, para avisarles de una campaña nueva (RF-19). */
    @Query("select p.usuario from Paciente p where p.usuario.estado = 'ACTIVO'")
    List<Usuario> usuariosActivos();
}
