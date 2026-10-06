package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.PersonalAdministrativo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalAdministrativoRepository extends JpaRepository<PersonalAdministrativo, Integer> {

    @EntityGraph(attributePaths = "usuario")
    Optional<PersonalAdministrativo> findByUsuario_IdUsuario(Integer idUsuario);
}
