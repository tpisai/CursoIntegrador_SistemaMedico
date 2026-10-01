package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.DocumentoMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentoMedicoRepository extends JpaRepository<DocumentoMedico, Integer> {

    @Query("""
            select dm from DocumentoMedico dm
            where dm.atencion.historia.paciente.idPaciente = :paciente and dm.estado = 'ACTIVO'
            order by dm.fechaCarga desc, dm.idDocumento
            """)
    List<DocumentoMedico> delPaciente(@Param("paciente") Integer idPaciente);
}
