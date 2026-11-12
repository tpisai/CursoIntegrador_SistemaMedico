package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Atencion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionRepository extends JpaRepository<Atencion, Integer> {

    boolean existsByCita_IdCita(Integer idCita);
    Optional<Atencion> findByCita_IdCita(Integer idCita);

    @EntityGraph(attributePaths = {
            "historia.paciente.usuario",
            "cita.horario",
            "doctor.usuario",
            "doctor.especialidad",
            "medicamentos"
    })
    List<Atencion> findByHistoria_IdHistoriaOrderByFechaAtencionDesc(Integer idHistoria);
}
