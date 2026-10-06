package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Integer> {

    Optional<HistoriaClinica> findByPaciente_IdPaciente(Integer idPaciente);
}
