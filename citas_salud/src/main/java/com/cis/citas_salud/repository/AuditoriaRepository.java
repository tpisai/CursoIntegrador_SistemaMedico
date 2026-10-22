package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    Optional<Auditoria> findFirstByAccionAndEntidadAndIdRegistroAndResultadoOrderByFechaDesc(
            String accion, String entidad, Integer idRegistro, String resultado);
}
