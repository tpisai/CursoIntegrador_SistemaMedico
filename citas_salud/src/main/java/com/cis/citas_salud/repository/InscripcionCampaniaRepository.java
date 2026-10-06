package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.InscripcionCampania;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionCampaniaRepository extends JpaRepository<InscripcionCampania, Integer> {

    boolean existsByCampania_IdCampaniaAndPaciente_IdPaciente(Integer idCampania, Integer idPaciente);

    @Query("select i from InscripcionCampania i join fetch i.campania where i.paciente.idPaciente = :paciente")
    List<InscripcionCampania> delPaciente(@Param("paciente") Integer idPaciente);

    @Query("""
            select i from InscripcionCampania i
            join fetch i.campania
            join fetch i.paciente p
            join fetch p.usuario
            where i.idInscripcion = :id
            """)
    Optional<InscripcionCampania> buscarConDetalle(@Param("id") Integer idInscripcion);

    /** [idCampania, inscritos] por campaña, en una sola consulta. */
    @Query("""
            select i.campania.idCampania, count(i) from InscripcionCampania i
            where i.estado <> 'CANCELADO'
            group by i.campania.idCampania
            """)
    List<Object[]> contarInscritos();
}
