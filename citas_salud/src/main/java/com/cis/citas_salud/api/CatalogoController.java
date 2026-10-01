package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.CitaDto.ConsultorioResponse;
import com.cis.citas_salud.api.dto.CitaDto.DisponibilidadResponse;
import com.cis.citas_salud.api.dto.CitaDto.DoctorResponse;
import com.cis.citas_salud.api.dto.CitaDto.EspecialidadResponse;
import com.cis.citas_salud.api.dto.CitaDto.HorarioResponse;
import com.cis.citas_salud.service.CatalogoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Consulta pública de disponibilidad (RF-05). No requiere sesión: el registro también usa las especialidades. */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/especialidades")
    public List<EspecialidadResponse> especialidades() {
        return catalogoService.especialidades();
    }

    @GetMapping("/doctores")
    public List<DoctorResponse> doctores(@RequestParam("especialidad") Integer idEspecialidad) {
        return catalogoService.doctores(idEspecialidad);
    }

    @GetMapping("/doctores/{id}/consultorios")
    public List<ConsultorioResponse> consultorios(@PathVariable("id") Integer idDoctor) {
        return catalogoService.consultorios(idDoctor);
    }

    @GetMapping("/horarios/disponibilidad")
    public List<DisponibilidadResponse> disponibilidad(@RequestParam("doctor") Integer idDoctor,
                                                       @RequestParam("consultorio") Integer idConsultorio,
                                                       @RequestParam("mes") String mes) {
        return catalogoService.disponibilidad(idDoctor, idConsultorio, mes);
    }

    @GetMapping("/horarios")
    public List<HorarioResponse> horarios(@RequestParam("doctor") Integer idDoctor,
                                          @RequestParam("consultorio") Integer idConsultorio,
                                          @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogoService.horarios(idDoctor, idConsultorio, fecha);
    }
}
