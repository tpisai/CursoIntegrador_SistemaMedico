package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Atencion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Comprobaciones previas a generar un reporte clínico: de quién es la atención y si tiene derivación. */
public interface ReporteClinicoRepository extends Repository<Atencion, Integer> {

    @Query("select count(a) > 0 from Atencion a where a.idAtencion = :atencion and a.doctor.idDoctor = :doctor")
    boolean esDelDoctor(@Param("atencion") Integer idAtencion, @Param("doctor") Integer idDoctor);

    @Query("""
            select count(s) > 0 from Solicitud s, Atencion a
            where a.idAtencion = :atencion and s.cita = a.cita and s.tipo = 'DERIVACION'
            """)
    boolean tieneDerivacion(@Param("atencion") Integer idAtencion);

    /** De las atenciones dadas, las que tienen una solicitud de derivación. */
    @Query("""
            select a.idAtencion from Atencion a, Solicitud s
            where a.idAtencion in :atenciones and s.cita = a.cita and s.tipo = 'DERIVACION'
            """)
    List<Integer> conDerivacion(@Param("atenciones") Collection<Integer> idsAtencion);
}
