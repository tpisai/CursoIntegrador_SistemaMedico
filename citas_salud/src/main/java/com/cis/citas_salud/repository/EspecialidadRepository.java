package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {

    List<Especialidad> findByEstadoOrderByIdEspecialidad(String estado);
}
