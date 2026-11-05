package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.DetalleMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleMedicamentoRepository extends JpaRepository<DetalleMedicamento, Integer> {
    List<DetalleMedicamento> findByAtencion_IdAtencionOrderByOrdenAsc(Integer idAtencion);
}
