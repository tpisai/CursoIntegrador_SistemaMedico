package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.RecuperacionPin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RecuperacionPinRepository extends JpaRepository<RecuperacionPin, Integer> {

    /** Bloquea la fila para que dos intentos simultáneos no se salten el límite. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecuperacionPin> findByUsuario_IdUsuario(Integer idUsuario);
}
