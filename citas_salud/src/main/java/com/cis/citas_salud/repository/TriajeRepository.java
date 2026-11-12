package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Triaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TriajeRepository extends JpaRepository<Triaje, Integer> {
    Optional<Triaje>
    findFirstByHistoriaClinica_IdHistoriaAndFechaTriajeLessThanEqualOrderByFechaTriajeDesc(
            Integer idHistoria,
            LocalDateTime fechaAtencion
    );
}
