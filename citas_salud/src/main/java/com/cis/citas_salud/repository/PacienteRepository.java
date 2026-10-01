package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {

    Optional<Paciente> findByUsuario_IdUsuario(Integer idUsuario);
}
