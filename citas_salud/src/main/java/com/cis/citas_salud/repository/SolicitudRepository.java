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

    /** Todas las solicitudes para el panel, de la más reciente a la más antigua. */
    @Query("""
            select s from Solicitud s
            join fetch s.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch s.cita c
            join fetch c.horario h
            join fetch h.consultorio
            join fetch c.paciente p
            join fetch p.usuario
            order by s.fechaSolicitud desc
            """)
    List<Solicitud> recientes();

    /** Cambios de fecha aprobados que el paciente todavía no resolvió. */
    @Query("""
            select s from Solicitud s
            join fetch s.cita c
            join fetch c.horario h
            join fetch h.consultorio
            join fetch h.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch c.paciente p
            join fetch p.usuario
            where s.tipo = 'CAMBIO_HORARIO' and s.estado = 'APROBADA' and p.idPaciente = :paciente
            order by s.fechaRespuesta
            """)
    List<Solicitud> cambiosAprobadosDelPaciente(@Param("paciente") Integer idPaciente);

    /** Historial de cambios de fecha del paciente, con lo necesario para mostrar la constancia. */
    @Query("""
            select s from Solicitud s
            join fetch s.cita c
            join fetch c.horario h
            join fetch h.consultorio
            join fetch h.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch c.paciente p
            join fetch p.usuario
            where s.tipo = 'CAMBIO_HORARIO' and p.idPaciente = :paciente
            order by s.fechaSolicitud desc
            """)
    List<Solicitud> cambiosDelPaciente(@Param("paciente") Integer idPaciente);

    /** Solicitud con doctor, cita, paciente y consultorio para la constancia en PDF. */
    @Query("""
            select s from Solicitud s
            join fetch s.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch s.cita c
            join fetch c.horario h
            join fetch h.consultorio
            join fetch c.paciente p
            join fetch p.usuario
            where s.idSolicitud = :id
            """)
    Optional<Solicitud> buscarConDetalle(@Param("id") Integer idSolicitud);

    /** Bloquea la solicitud mientras administración la responde, para no aprobarla dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.idSolicitud = :id")
    Optional<Solicitud> bloquear(@Param("id") Integer idSolicitud);
}
