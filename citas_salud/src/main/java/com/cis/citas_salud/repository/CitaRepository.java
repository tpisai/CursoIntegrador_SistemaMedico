package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Integer> {

    // Trae de una vez lo que muestran las pantallas (evita una consulta por fila).
    String CON_DETALLE = """
            select c from Cita c
            join fetch c.horario h
            join fetch h.consultorio
            join fetch h.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            join fetch c.paciente p
            join fetch p.usuario
            """;

    @Query(value = "select nextval('seq_ticket_cita')", nativeQuery = true)
    long siguienteTicket();

    @Query(CON_DETALLE + " where c.idCita = :id")
    Optional<Cita> buscarConDetalle(@Param("id") Integer idCita);

    @Query(CON_DETALLE + """
             where p.idPaciente = :paciente and c.estado in :estados and h.fecha >= :desde
             order by h.fecha, h.horaInicio
            """)
    List<Cita> delPacienteDesde(@Param("paciente") Integer idPaciente,
                                @Param("estados") Collection<String> estados,
                                @Param("desde") LocalDate desde);

    @Query(CON_DETALLE + """
             where p.idPaciente = :paciente and c.estado in :estados
             order by h.fecha desc, h.horaInicio desc
            """)
    List<Cita> historialDelPaciente(@Param("paciente") Integer idPaciente,
                                    @Param("estados") Collection<String> estados);

    @Query(CON_DETALLE + """
             where d.idDoctor = :doctor and c.estado in :estados and h.fecha between :desde and :hasta
             order by h.fecha, h.horaInicio
            """)
    List<Cita> delDoctorEntre(@Param("doctor") Integer idDoctor,
                              @Param("estados") Collection<String> estados,
                              @Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta);

    // Indicadores del panel de administración.
    @Query("select count(c) from Cita c where c.estado in :estados and c.horario.fecha between :desde and :hasta")
    long contarEntre(@Param("estados") Collection<String> estados,
                     @Param("desde") LocalDate desde,
                     @Param("hasta") LocalDate hasta);
}
