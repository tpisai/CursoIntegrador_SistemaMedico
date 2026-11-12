package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.ResultadoExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResultadoExamenRepository extends JpaRepository<ResultadoExamen, Integer> {

    boolean existsByAtencion_IdAtencion(Integer idAtencion);

    @Query("""
            select r from ResultadoExamen r
            where r.atencion.idAtencion in :atenciones
            order by r.orden, r.idResultado
            """)
    List<ResultadoExamen> deAtenciones(@Param("atenciones") Collection<Integer> idsAtencion);
}
