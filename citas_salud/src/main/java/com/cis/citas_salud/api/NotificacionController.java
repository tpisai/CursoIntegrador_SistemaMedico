package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.NotificacionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Notificaciones del usuario que inició sesión (RF-17, RF-22). */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionResponse> listar(UsuarioSesion sesion) {
        return notificacionService.listar(sesion.idUsuario());
    }

    @PatchMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeida(UsuarioSesion sesion, @PathVariable("id") Integer idNotificacion) {
        notificacionService.marcarLeida(sesion.idUsuario(), idNotificacion);
    }

    @PatchMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(UsuarioSesion sesion) {
        notificacionService.marcarTodasLeidas(sesion.idUsuario());
    }
}
