package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    boolean existsByCita_IdCitaAndTipoAndEstado(Integer idCita, String tipo, String estado);

    @Query("""
            select s from Solicitud s
            join fetch s.cita c
            join fetch c.horario h
            join fetch c.paciente p
            join fetch p.usuario
            where s.doctor.idDoctor = :doctor
            order by s.fechaSolicitud desc
            """)
    List<Solicitud> delDoctor(@Param("doctor") Integer idDoctor);
}
