package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.SolicitudDto.CambioPacienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.CambioPendienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolverCambioRequest;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolucionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cambios de fecha vistos por el paciente (RF-17) y su constancia en PDF, que también descargan
 * el doctor solicitante y administración.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/pendientes")
    public List<CambioPendienteResponse> pendientes(UsuarioSesion sesion) {
        return solicitudService.pendientes(sesion);
    }

    @GetMapping("/mias")
    public List<CambioPacienteResponse> mias(UsuarioSesion sesion) {
        return solicitudService.mias(sesion);
    }

    @PostMapping("/{id}/resolver")
    public ResolucionResponse resolver(UsuarioSesion sesion, @PathVariable("id") Integer idSolicitud,
                                       @Valid @RequestBody ResolverCambioRequest datos) {
        return solicitudService.resolver(sesion, idSolicitud, datos);
    }

    @GetMapping("/{id}/constancia")
    public ResponseEntity<byte[]> constancia(UsuarioSesion sesion, @PathVariable("id") Integer idSolicitud) {
        return CitaController.descarga(solicitudService.constancia(sesion, idSolicitud));
    }
}
