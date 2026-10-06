package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Campania;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CampaniaRepository extends JpaRepository<Campania, Integer> {

    /** Campañas que los pacientes todavía pueden ver: activas o planificadas y sin terminar. */
    @Query("""
            select c from Campania c
            where c.estado in :estados and c.fechaFin >= :hoy
            order by c.fechaInicio, c.idCampania
            """)
    List<Campania> vigentes(@Param("estados") Collection<String> estados, @Param("hoy") LocalDate hoy);

    List<Campania> findAllByOrderByFechaInicioDescIdCampaniaDesc();

    long countByEstadoInAndFechaFinGreaterThanEqual(Collection<String> estados, LocalDate hoy);

    /**
     * Descuenta un cupo en una sola sentencia: si dos pacientes piden el último cupo a la vez,
     * solo uno lo obtiene (devuelve 0 filas al otro).
     */
    @Modifying(flushAutomatically = true)
    @Query("update Campania c set c.cuposDisponibles = c.cuposDisponibles - 1 where c.idCampania = :id and c.cuposDisponibles > 0")
    int tomarCupo(@Param("id") Integer idCampania);
}
