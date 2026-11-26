package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.AdminDto.CampaniaAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.CrearCampaniaRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosResponse;
import com.cis.citas_salud.api.dto.AdminDto.ReporteOcupacionResponse;
import com.cis.citas_salud.api.dto.AdminDto.ReporteResponse;
import com.cis.citas_salud.api.dto.AdminDto.ResponderSolicitudRequest;
import com.cis.citas_salud.api.dto.AdminDto.ResumenResponse;
import com.cis.citas_salud.api.dto.AdminDto.SolicitudAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.TicketResponse;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.AdminService;
import com.cis.citas_salud.service.CampaniaService;
import com.cis.citas_salud.service.RecepcionService;
import com.cis.citas_salud.service.ReporteClinicoService;
import com.cis.citas_salud.service.ReporteIntegralCitasService;
import com.cis.citas_salud.service.ReportePrescripcionService;
import com.cis.citas_salud.service.ReporteService;
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

/** Panel de administración y recepción (RF-12, RF-13, RF-17, RF-18, RF-20). Solo rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final CampaniaService campaniaService;
    private final RecepcionService recepcionService;
    private final ReporteService reporteService;
    private final ReporteIntegralCitasService reporteIntegralCitasService;
    private final ReporteClinicoService reporteClinicoService;
    private final ReportePrescripcionService reportePrescripcionService;

    public AdminController(AdminService adminService, CampaniaService campaniaService,
                           RecepcionService recepcionService, ReporteService reporteService,
                           ReporteClinicoService reporteClinicoService,
                           ReporteIntegralCitasService reporteIntegralCitasService,
                           ReportePrescripcionService reportePrescripcionService) {
        this.adminService = adminService;
        this.campaniaService = campaniaService;
        this.recepcionService = recepcionService;
        this.reporteService = reporteService;
        this.reporteClinicoService = reporteClinicoService;
        this.reporteIntegralCitasService = reporteIntegralCitasService;
        this.reportePrescripcionService = reportePrescripcionService;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen(UsuarioSesion sesion) {
        return adminService.resumen(sesion);
    }

    @GetMapping("/reportes/citas")
    public ReporteResponse reporteCitas(
            UsuarioSesion sesion,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.citas(sesion, desde, hasta);
    }

    @GetMapping("/reportes/horarios")
    public ReporteOcupacionResponse reporteOcupacionHorarios(
            UsuarioSesion sesion,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.ocupacionHorarios(sesion, desde, hasta);
    }

    @GetMapping("/reportes/citas-integral/pdf")
    public ResponseEntity<byte[]> reporteIntegralCitas(
            UsuarioSesion sesion,
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "secciones", required = false) List<String> secciones) {
        return CitaController.descarga(reporteIntegralCitasService.pdf(sesion, desde, hasta, secciones));
    }

    @GetMapping("/reportes/prescripcion")
    public ReportePrescripcionDto.ReporteResponse reportePrescripcion(
            UsuarioSesion sesion,
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "idEspecialidad", required = false) Integer idEspecialidad,
            @RequestParam(value = "idDoctor", required = false) Integer idDoctor) {
        return reportePrescripcionService.reporte(sesion, desde, hasta, idEspecialidad, idDoctor);
    }

    @GetMapping("/reportes/prescripcion/pdf")
    public ResponseEntity<byte[]> reportePrescripcionPdf(
            UsuarioSesion sesion,
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "idEspecialidad", required = false) Integer idEspecialidad,
            @RequestParam(value = "idDoctor", required = false) Integer idDoctor) {
        return CitaController.descarga(
                reportePrescripcionService.pdf(sesion, desde, hasta, idEspecialidad, idDoctor));
    }

    @GetMapping("/reportes/prescripcion/csv")
    public ResponseEntity<byte[]> reportePrescripcionCsv(
            UsuarioSesion sesion,
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "idEspecialidad", required = false) Integer idEspecialidad,
            @RequestParam(value = "idDoctor", required = false) Integer idDoctor) {
        return CitaController.descarga(
                reportePrescripcionService.csv(sesion, desde, hasta, idEspecialidad, idDoctor));
    }

    @GetMapping("/solicitudes")
    public List<SolicitudAdminResponse> solicitudes(UsuarioSesion sesion) {
        return adminService.solicitudes(sesion);
    }

    @GetMapping("/solicitudes/{id}/referencia")
    public ResponseEntity<byte[]> referencia(UsuarioSesion sesion,
                                              @PathVariable("id") Integer idSolicitud) {
        return CitaController.descarga(reporteClinicoService.deReferenciaAdministracion(sesion, idSolicitud));
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