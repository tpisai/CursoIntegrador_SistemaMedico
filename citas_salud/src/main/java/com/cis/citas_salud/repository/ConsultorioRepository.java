package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultorioRepository extends JpaRepository<Consultorio, Integer> {
}