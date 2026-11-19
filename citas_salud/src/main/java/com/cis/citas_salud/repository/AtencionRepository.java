package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Atencion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AtencionRepository extends JpaRepository<Atencion, Integer> {

    boolean existsByCita_IdCita(Integer idCita);
    Optional<Atencion> findByCita_IdCita(Integer idCita);

    @EntityGraph(attributePaths = "cita")
    List<Atencion> findByCita_IdCitaIn(Collection<Integer> idsCita);

    @EntityGraph(attributePaths = {
            "historia.paciente.usuario",
            "cita.horario",
            "doctor.usuario",
            "doctor.especialidad",
            "medicamentos"
    })
    List<Atencion> findByHistoria_IdHistoriaOrderByFechaAtencionDesc(Integer idHistoria);

    @EntityGraph(attributePaths = {
            "cita.horario.doctor.usuario",
            "cita.horario.doctor.especialidad",
            "cita.horario.consultorio",
            "cita.paciente.usuario"
    })
    List<Atencion> findByDoctor_IdDoctorAndCita_EstadoOrderByFechaAtencionDesc(Integer idDoctor, String estado);
}
