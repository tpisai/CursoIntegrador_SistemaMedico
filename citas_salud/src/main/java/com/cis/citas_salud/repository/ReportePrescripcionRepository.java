package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Atencion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Agregaciones de solo lectura para el reporte integral de morbilidad y prescripción. */
public interface ReportePrescripcionRepository extends Repository<Atencion, Integer> {

    @Query(value = """
            select count(distinct a.id_atencion),
                   count(m.id_detalle),
                   count(distinct case when m.id_detalle is not null then a.id_atencion end)
            from atencion a
            join doctor d on d.id_doctor = a.id_doctor
            left join detalle_medicamento m on m.id_atencion = a.id_atencion
            where a.fecha_atencion >= :desde and a.fecha_atencion < :hasta
              and (cast(:idEspecialidad as integer) is null or d.id_especialidad = :idEspecialidad)
              and (cast(:idDoctor as integer) is null or d.id_doctor = :idDoctor)
            """, nativeQuery = true)
    List<Object[]> resumen(@Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           @Param("idEspecialidad") Integer idEspecialidad,
                           @Param("idDoctor") Integer idDoctor);

    @Query(value = """
            select upper(trim(m.nombre_medicamento)) as nombre, count(*) as cantidad
            from atencion a
            join doctor d on d.id_doctor = a.id_doctor
            join detalle_medicamento m on m.id_atencion = a.id_atencion
            where a.fecha_atencion >= :desde and a.fecha_atencion < :hasta
              and trim(m.nombre_medicamento) <> ''
              and (cast(:idEspecialidad as integer) is null or d.id_especialidad = :idEspecialidad)
              and (cast(:idDoctor as integer) is null or d.id_doctor = :idDoctor)
            group by upper(trim(m.nombre_medicamento))
            order by cantidad desc, nombre
            limit 10
            """, nativeQuery = true)
    List<Object[]> topMedicamentos(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   @Param("idEspecialidad") Integer idEspecialidad,
                                   @Param("idDoctor") Integer idDoctor);

    @Query(value = """
            select upper(trim(a.diagnostico)) as nombre, count(*) as atenciones
            from atencion a
            join doctor d on d.id_doctor = a.id_doctor
            where a.fecha_atencion >= :desde and a.fecha_atencion < :hasta
              and a.diagnostico is not null and trim(a.diagnostico) <> ''
              and (cast(:idEspecialidad as integer) is null or d.id_especialidad = :idEspecialidad)
              and (cast(:idDoctor as integer) is null or d.id_doctor = :idDoctor)
            group by upper(trim(a.diagnostico))
            order by atenciones desc, nombre
            limit 10
            """, nativeQuery = true)
    List<Object[]> topDiagnosticos(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   @Param("idEspecialidad") Integer idEspecialidad,
                                   @Param("idDoctor") Integer idDoctor);

    @Query(value = """
            select d.id_doctor, u.nombres || ' ' || u.apellidos as doctor, e.nombre as especialidad,
                   count(distinct a.id_atencion) as recetas
            from atencion a
            join doctor d on d.id_doctor = a.id_doctor
            join usuario u on u.id_usuario = d.id_usuario
            join especialidad e on e.id_especialidad = d.id_especialidad
            join detalle_medicamento m on m.id_atencion = a.id_atencion
            where a.fecha_atencion >= :desde and a.fecha_atencion < :hasta
              and (cast(:idEspecialidad as integer) is null or d.id_especialidad = :idEspecialidad)
              and (cast(:idDoctor as integer) is null or d.id_doctor = :idDoctor)
            group by d.id_doctor, u.nombres, u.apellidos, e.nombre
            order by recetas desc, u.apellidos, u.nombres
            """, nativeQuery = true)
    List<Object[]> porMedico(@Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             @Param("idEspecialidad") Integer idEspecialidad,
                             @Param("idDoctor") Integer idDoctor);

    @Query(value = """
            select cast(date_trunc('week', a.fecha_atencion) as date) as semana,
                   count(distinct a.id_atencion) as recetas
            from atencion a
            join doctor d on d.id_doctor = a.id_doctor
            join detalle_medicamento m on m.id_atencion = a.id_atencion
            where a.fecha_atencion >= :desde and a.fecha_atencion < :hasta
              and (cast(:idEspecialidad as integer) is null or d.id_especialidad = :idEspecialidad)
              and (cast(:idDoctor as integer) is null or d.id_doctor = :idDoctor)
            group by date_trunc('week', a.fecha_atencion)
            order by semana
            """, nativeQuery = true)
    List<Object[]> tendenciaSemanal(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   @Param("idEspecialidad") Integer idEspecialidad,
                                   @Param("idDoctor") Integer idDoctor);
}
