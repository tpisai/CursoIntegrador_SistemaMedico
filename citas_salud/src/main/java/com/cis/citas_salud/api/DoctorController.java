package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.DoctorDto.CambioFechaRequest;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import com.cis.citas_salud.api.dto.DoctorDto.DerivacionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.HorarioLibreResponse;
import com.cis.citas_salud.api.dto.DoctorDto.SolicitudResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Panel del doctor (RF-14 a RF-16). */
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/citas")
    public List<CitaAsignadaResponse> citasDelDia(
            UsuarioSesion sesion,
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return doctorService.citasDelDia(sesion, fecha != null ? fecha : LocalDate.now());
    }

    @GetMapping("/citas/proximas")
    public List<CitaAsignadaResponse> citasProximas(UsuarioSesion sesion) {
        return doctorService.citasProximas(sesion);
    }

    @GetMapping("/horarios-libres")
    public List<HorarioLibreResponse> horariosLibres(UsuarioSesion sesion) {
        return doctorService.horariosLibres(sesion);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudResponse> solicitudes(UsuarioSesion sesion) {
        return doctorService.solicitudes(sesion);
    }

    @PostMapping("/solicitudes/cambio-fecha")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse cambioFecha(UsuarioSesion sesion, @Valid @RequestBody CambioFechaRequest datos) {
        return doctorService.solicitarCambioFecha(sesion, datos);
    }

    @PostMapping("/solicitudes/derivacion")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse derivacion(UsuarioSesion sesion, @Valid @RequestBody DerivacionRequest datos) {
        return doctorService.derivar(sesion, datos);
    }
}
