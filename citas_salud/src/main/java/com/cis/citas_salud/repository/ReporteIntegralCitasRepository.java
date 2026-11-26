package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Solicitud;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Agregaciones de solo lectura para el reporte integral administrativo. */
public interface ReporteIntegralCitasRepository extends Repository<Cita, Integer> {

    @Query("""
            select c.estado, count(c)
            from Cita c
            where c.horario.fecha between :desde and :hasta
            group by c.estado
            order by c.estado
            """)
    List<Object[]> conteoPorEstado(@Param("desde") java.time.LocalDate desde,
                                   @Param("hasta") java.time.LocalDate hasta);

    @Query("""
            select e.nombre, c.estado, count(c)
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
            group by e.nombre, c.estado
            order by e.nombre, c.estado
            """)
    List<Object[]> conteoPorEspecialidadYEstado(@Param("desde") java.time.LocalDate desde,
                                                @Param("hasta") java.time.LocalDate hasta);

    @Query("""
            select d.usuario.nombres, d.usuario.apellidos, e.nombre, count(c)
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
            group by d.idDoctor, d.usuario.nombres, d.usuario.apellidos, e.nombre
            order by count(c) desc, d.usuario.apellidos, d.usuario.nombres
            """)
    List<Object[]> doctoresConMasCitas(@Param("desde") java.time.LocalDate desde,
                                       @Param("hasta") java.time.LocalDate hasta);

    @Query("""
            select c.estado, count(c)
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
              and (:idEspecialidad is null or e.idEspecialidad = :idEspecialidad)
              and (:idDoctor is null or d.idDoctor = :idDoctor)
              and (:estado is null or c.estado = :estado)
            group by c.estado
            order by c.estado
            """)
    List<Object[]> integralConteoPorEstado(@Param("desde") java.time.LocalDate desde,
                                           @Param("hasta") java.time.LocalDate hasta,
                                           @Param("idEspecialidad") Integer idEspecialidad,
                                           @Param("idDoctor") Integer idDoctor,
                                           @Param("estado") String estado);

    @Query("""
            select h.fecha, c.estado, count(c)
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
              and (:idEspecialidad is null or e.idEspecialidad = :idEspecialidad)
              and (:idDoctor is null or d.idDoctor = :idDoctor)
              and (:estado is null or c.estado = :estado)
            group by h.fecha, c.estado
            order by h.fecha, c.estado
            """)
    List<Object[]> integralEvolucionDiaria(@Param("desde") java.time.LocalDate desde,
                                           @Param("hasta") java.time.LocalDate hasta,
                                           @Param("idEspecialidad") Integer idEspecialidad,
                                           @Param("idDoctor") Integer idDoctor,
                                           @Param("estado") String estado);

    @Query("""
            select e.nombre, c.estado, count(c)
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
              and (:idEspecialidad is null or e.idEspecialidad = :idEspecialidad)
              and (:idDoctor is null or d.idDoctor = :idDoctor)
              and (:estado is null or c.estado = :estado)
            group by e.nombre, c.estado
            order by e.nombre, c.estado
            """)
    List<Object[]> integralPorEspecialidad(@Param("desde") java.time.LocalDate desde,
                                           @Param("hasta") java.time.LocalDate hasta,
                                           @Param("idEspecialidad") Integer idEspecialidad,
                                           @Param("idDoctor") Integer idDoctor,
                                           @Param("estado") String estado);

    @Query("""
            select c.idCita, h.fecha, h.horaInicio, e.nombre, d.usuario.nombres, d.usuario.apellidos, c.estado
            from Cita c join c.horario h join h.doctor d join d.especialidad e
            where h.fecha between :desde and :hasta
              and (:idEspecialidad is null or e.idEspecialidad = :idEspecialidad)
              and (:idDoctor is null or d.idDoctor = :idDoctor)
              and (:estado is null or c.estado = :estado)
            order by h.fecha desc, h.horaInicio desc, c.idCita desc
            """)
    List<Object[]> integralDetalle(@Param("desde") java.time.LocalDate desde,
                                  @Param("hasta") java.time.LocalDate hasta,
                                  @Param("idEspecialidad") Integer idEspecialidad,
                                  @Param("idDoctor") Integer idDoctor,
                                  @Param("estado") String estado);

    @Query(value = """
            select coalesce(s.datos_adicionales::jsonb ->> 'decisionPaciente', 'PENDIENTE'), count(*)
            from solicitud s
            where s.tipo = 'CAMBIO_HORARIO'
              and s.fecha_solicitud >= :desde and s.fecha_solicitud < :hasta
              and s.estado = 'RESUELTA'
              and coalesce(s.datos_adicionales::jsonb ->> 'decisionPaciente', '') in ('MISMO_DOCTOR', 'OTRO_DOCTOR')
            group by coalesce(s.datos_adicionales::jsonb ->> 'decisionPaciente', 'PENDIENTE')
            order by 1
            """, nativeQuery = true)
    List<Object[]> cambiosDeFecha(@Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            select s.estado, count(*)
            from solicitud s
            where s.tipo = 'DERIVACION'
              and s.fecha_solicitud >= :desde and s.fecha_solicitud < :hasta
            group by s.estado
            order by s.estado
            """, nativeQuery = true)
    List<Object[]> derivaciones(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);
}
