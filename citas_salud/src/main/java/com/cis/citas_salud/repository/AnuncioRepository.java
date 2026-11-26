package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Anuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnuncioRepository extends JpaRepository<Anuncio, Integer> {

    /** Banners que hoy se muestran en el carrusel del paciente, el más reciente primero. */
    @Query("""
            select a from Anuncio a
            where a.activo = true and a.fechaInicio <= :hoy and a.fechaFin >= :hoy
            order by a.fechaInicio desc, a.idAnuncio desc
            """)
    List<Anuncio> vigentes(@Param("hoy") LocalDate hoy);

    List<Anuncio> findAllByOrderByFechaCreacionDescIdAnuncioDesc();
}
