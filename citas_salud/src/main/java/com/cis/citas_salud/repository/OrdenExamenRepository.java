package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.OrdenExamen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdenExamenRepository extends JpaRepository<OrdenExamen, Integer> {

    boolean existsByAtencion_Cita_IdCita(Integer idCita);

    @Query("select o.atencion.cita.idCita from OrdenExamen o where o.atencion.cita.idCita in :idsCita")
    List<Integer> findCitasConOrden(@Param("idsCita") Collection<Integer> idsCita);

    @EntityGraph(attributePaths = {
            "atencion.historia.paciente.usuario",
            "atencion.doctor.usuario",
            "atencion.doctor.especialidad"
    })
    Optional<OrdenExamen> findByAtencion_IdAtencion(Integer idAtencion);

    @EntityGraph(attributePaths = {
            "atencion.historia.paciente.usuario",
            "atencion.doctor.usuario",
            "atencion.doctor.especialidad"
    })
    Optional<OrdenExamen> findByAtencion_Cita_IdCita(Integer idCita);
}
