package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.AdminDto.CampaniaAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.CrearCampaniaRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosResponse;
import com.cis.citas_salud.api.dto.AdminDto.ResponderSolicitudRequest;
import com.cis.citas_salud.api.dto.AdminDto.ResumenResponse;
import com.cis.citas_salud.api.dto.AdminDto.SolicitudAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.TicketResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.AdminService;
import com.cis.citas_salud.service.CampaniaService;
import com.cis.citas_salud.service.RecepcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Panel de administración y recepción (RF-12, RF-13, RF-17, RF-18, RF-20). Solo rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final CampaniaService campaniaService;
    private final RecepcionService recepcionService;

    public AdminController(AdminService adminService, CampaniaService campaniaService,
                           RecepcionService recepcionService) {
        this.adminService = adminService;
        this.campaniaService = campaniaService;
        this.recepcionService = recepcionService;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen(UsuarioSesion sesion) {
        return adminService.resumen(sesion);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudAdminResponse> solicitudesPendientes(UsuarioSesion sesion) {
        return adminService.solicitudesPendientes(sesion);
    }

    @PatchMapping("/solicitudes/{id}")
    public SolicitudAdminResponse responder(UsuarioSesion sesion, @PathVariable("id") Integer idSolicitud,
                                            @Valid @RequestBody ResponderSolicitudRequest datos) {
        return adminService.responder(sesion, idSolicitud, datos);
    }

    @PostMapping("/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    public CrearHorariosResponse crearHorarios(UsuarioSesion sesion, @Valid @RequestBody CrearHorariosRequest datos) {
        return adminService.crearHorarios(sesion, datos);
    }

    @GetMapping("/tickets/{numero}")
    public TicketResponse consultarTicket(UsuarioSesion sesion, @PathVariable("numero") String numeroTicket) {
        return recepcionService.consultar(sesion, numeroTicket);
    }

    @PostMapping("/tickets/{numero}/validacion")
    public TicketResponse validarTicket(UsuarioSesion sesion, @PathVariable("numero") String numeroTicket) {
        return recepcionService.validar(sesion, numeroTicket);
    }

    @GetMapping("/campanias")
    public List<CampaniaAdminResponse> campanias(UsuarioSesion sesion) {
        return campaniaService.listarParaAdmin(sesion);
    }

    @PostMapping("/campanias")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaniaAdminResponse crearCampania(UsuarioSesion sesion, @Valid @RequestBody CrearCampaniaRequest datos) {
        return campaniaService.crear(sesion, datos);
    }
}
