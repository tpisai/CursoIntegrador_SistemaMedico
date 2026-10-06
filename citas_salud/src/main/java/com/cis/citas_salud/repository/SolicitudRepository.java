package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Solicitud;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    boolean existsByCita_IdCitaAndTipoAndEstado(Integer idCita, String tipo, String estado);

    long countByEstado(String estado);

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

    // Para el panel de administración: doctor, especialidad, cita y paciente en una sola consulta.
    @Query("""
            select s from Solicitud s
            join fetch s.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch s.cita c
            join fetch c.horario h
            join fetch c.paciente p
            join fetch p.usuario
            where s.estado = :estado
            order by s.fechaSolicitud
            """)
    List<Solicitud> conEstado(@Param("estado") String estado);

    /** Bloquea la solicitud mientras administración la responde, para no aprobarla dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.idSolicitud = :id")
    Optional<Solicitud> bloquear(@Param("id") Integer idSolicitud);
}
