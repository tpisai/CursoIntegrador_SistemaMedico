package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {
}