package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.NotificacionResponse;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notificaciones dentro del sistema (RF-17, RF-22). El envío por SMS, WhatsApp o correo
 * se conectará en el módulo de notificaciones externas.
 */
@Service
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /** tipo: una de las constantes de {@link Notificacion} (CITA, SOLICITUD, SISTEMA...). */
    public void crear(Usuario usuario, String tipo, String titulo, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacionRepository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Integer idUsuario) {
        return notificacionRepository.findTop30ByUsuario_IdUsuarioOrderByFechaEnvioDescIdNotificacionDesc(idUsuario)
                .stream()
                .map(n -> new NotificacionResponse(
                        n.getIdNotificacion(),
                        n.getTipo(),
                        n.getTitulo(),
                        n.getMensaje(),
                        n.getLeida(),
                        Formato.fechaHora(n.getFechaEnvio())))
                .toList();
    }

    public void marcarLeida(Integer idUsuario, Integer idNotificacion) {
        Notificacion notificacion = notificacionRepository
                .findByIdNotificacionAndUsuario_IdUsuario(idNotificacion, idUsuario)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa notificación."));
        notificacion.setLeida(true);
    }

    public void marcarTodasLeidas(Integer idUsuario) {
        notificacionRepository.marcarTodasLeidas(idUsuario);
    }
}
