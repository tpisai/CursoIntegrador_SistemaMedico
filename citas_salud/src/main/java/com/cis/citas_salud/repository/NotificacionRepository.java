package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findTop30ByUsuario_IdUsuarioOrderByFechaEnvioDescIdNotificacionDesc(Integer idUsuario);

    // Evita repetir un recordatorio: el título identifica la cita (ticket) o la campaña.
    boolean existsByUsuario_IdUsuarioAndTipoAndTitulo(Integer idUsuario, String tipo, String titulo);

    Optional<Notificacion> findByIdNotificacionAndUsuario_IdUsuario(Integer idNotificacion, Integer idUsuario);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuario.idUsuario = :usuario and n.leida = false")
    int marcarTodasLeidas(@Param("usuario") Integer idUsuario);
}
