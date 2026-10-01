package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Doctor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    @EntityGraph(attributePaths = {"usuario", "especialidad"})
    Optional<Doctor> findByUsuario_IdUsuario(Integer idUsuario);

    @EntityGraph(attributePaths = "usuario")
    List<Doctor> findByEspecialidad_IdEspecialidadAndEstadoOrderByIdDoctor(Integer idEspecialidad, String estado);

    @EntityGraph(attributePaths = "consultorios")
    List<Doctor> findByEstado(String estado);

    boolean existsByCmp(String cmp);
}
