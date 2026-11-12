package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.ConsultaDto.AntecedentesRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.ConsultaResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.PacienteResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.ReporteExamenesRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.ResultadoExamenResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.ConsultaService;
import com.cis.citas_salud.service.ReporteClinicoService;
import com.cis.citas_salud.service.ReporteClinicoService.Documento;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Página de consulta del doctor: agenda por fechas, historia clínica del paciente y reporte de exámenes. */
@RestController
@RequestMapping("/api/doctor")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final ReporteClinicoService reporteClinicoService;

    public ConsultaController(ConsultaService consultaService, ReporteClinicoService reporteClinicoService) {
        this.consultaService = consultaService;
        this.reporteClinicoService = reporteClinicoService;
    }

    @GetMapping("/agenda")
    public List<CitaAsignadaResponse> agenda(
            UsuarioSesion sesion,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultaService.agenda(sesion, desde, hasta);
    }

    @GetMapping("/citas/{id}/consulta")
    public ConsultaResponse consulta(UsuarioSesion sesion, @PathVariable("id") Integer idCita) {
        return consultaService.consulta(sesion, idCita);
    }

    @PatchMapping("/citas/{id}/antecedentes")
    public PacienteResponse antecedentes(UsuarioSesion sesion, @PathVariable("id") Integer idCita,
                                         @Valid @RequestBody AntecedentesRequest datos) {
        return consultaService.actualizarAntecedentes(sesion, idCita, datos);
    }

    @PostMapping("/atenciones/{id}/resultados")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResultadoExamenResponse> resultados(UsuarioSesion sesion, @PathVariable("id") Integer idAtencion,
                                                    @Valid @RequestBody ReporteExamenesRequest datos) {
        return consultaService.registrarResultados(sesion, idAtencion, datos);
    }

    @GetMapping("/documentos/{id}/descarga")
    public ResponseEntity<byte[]> documento(UsuarioSesion sesion, @PathVariable("id") Integer idDocumento) {
        return CitaController.descarga(consultaService.documento(sesion, idDocumento));
    }

    // ----- Documentos clínicos en PDF (plantillas con consulta SQL) -----

    @GetMapping("/atenciones/{id}/ficha-consulta")
    public ResponseEntity<byte[]> fichaConsulta(UsuarioSesion sesion, @PathVariable("id") Integer idAtencion) {
        return CitaController.descarga(reporteClinicoService.deAtencion(sesion, idAtencion, Documento.FICHA_CONSULTA));
    }

    @GetMapping("/atenciones/{id}/informe-medico")
    public ResponseEntity<byte[]> informeMedico(UsuarioSesion sesion, @PathVariable("id") Integer idAtencion) {
        return CitaController.descarga(reporteClinicoService.deAtencion(sesion, idAtencion, Documento.INFORME_MEDICO));
    }

    @GetMapping("/atenciones/{id}/referencia")
    public ResponseEntity<byte[]> referencia(UsuarioSesion sesion, @PathVariable("id") Integer idAtencion) {
        return CitaController.descarga(reporteClinicoService.deAtencion(sesion, idAtencion, Documento.REFERENCIA));
    }

    @GetMapping("/citas/{id}/fichas-consulta")
    public ResponseEntity<byte[]> fichasDelPaciente(UsuarioSesion sesion, @PathVariable("id") Integer idCita) {
        return CitaController.descarga(reporteClinicoService.fichasDelPaciente(sesion, idCita));
    }
}
