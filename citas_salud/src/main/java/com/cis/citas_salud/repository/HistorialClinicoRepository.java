package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.DetalleMedicamento;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Lecturas de la historia clínica que ve el doctor al atender (trae de una vez lo que muestra la pantalla). */
public interface HistorialClinicoRepository extends Repository<Atencion, Integer> {

    @Query("""
            select a from Atencion a
            join fetch a.cita
            join fetch a.doctor d
            join fetch d.usuario
            join fetch d.especialidad
            where a.historia.idHistoria = :historia
            order by a.fechaAtencion desc, a.idAtencion desc
            """)
    List<Atencion> atenciones(@Param("historia") Integer idHistoria);

    @Query("""
            select m from DetalleMedicamento m
            where m.atencion.idAtencion in :atenciones
            order by m.orden, m.idDetalle
            """)
    List<DetalleMedicamento> medicamentos(@Param("atenciones") Collection<Integer> idsAtencion);
}
