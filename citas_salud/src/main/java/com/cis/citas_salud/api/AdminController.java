package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.AdminDto.CampaniaAdminResponse;
import com.cis.citas_salud.api.dto.AnuncioDto.AnuncioAdminResponse;
import com.cis.citas_salud.api.dto.AnuncioDto.CrearAnuncioRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearCampaniaRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosResponse;
import com.cis.citas_salud.api.dto.AdminDto.ReporteOcupacionResponse;
import com.cis.citas_salud.api.dto.AdminDto.ReporteResponse;
import com.cis.citas_salud.api.dto.AdminDto.ResponderSolicitudRequest;
import com.cis.citas_salud.api.dto.AdminDto.ResumenResponse;
import com.cis.citas_salud.api.dto.AdminDto.SolicitudAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.TicketResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.AdminService;
import com.cis.citas_salud.service.AnuncioService;
import com.cis.citas_salud.service.CampaniaService;
import com.cis.citas_salud.service.RecepcionService;
import com.cis.citas_salud.service.ReporteClinicoService;
import com.cis.citas_salud.service.ReporteIntegralCitasService;
import com.cis.citas_salud.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final AnuncioService anuncioService;

    public AdminController(AdminService adminService, CampaniaService campaniaService,
                           RecepcionService recepcionService, ReporteService reporteService,
                           ReporteClinicoService reporteClinicoService,
                           ReporteIntegralCitasService reporteIntegralCitasService,
                           AnuncioService anuncioService) {
        this.adminService = adminService;
        this.campaniaService = campaniaService;
        this.recepcionService = recepcionService;
        this.reporteService = reporteService;
        this.reporteClinicoService = reporteClinicoService;
        this.reporteIntegralCitasService = reporteIntegralCitasService;
        this.anuncioService = anuncioService;
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

    // ----- Carrusel de eventos del portal del paciente -----

    @GetMapping("/anuncios")
    public List<AnuncioAdminResponse> anuncios(UsuarioSesion sesion) {
        return anuncioService.listarParaAdmin(sesion);
    }

    /** Multipart: los datos del anuncio como campos y el banner en "imagen" (JPG o PNG, 1200 x 640 px). */
    @PostMapping("/anuncios")
    @ResponseStatus(HttpStatus.CREATED)
    public AnuncioAdminResponse crearAnuncio(
            UsuarioSesion sesion,
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "etiqueta", required = false) String etiqueta,
            @RequestParam(value = "enlace", required = false) String enlace,
            @RequestParam(value = "textoBoton", required = false) String textoBoton,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen) {
        CrearAnuncioRequest datos = new CrearAnuncioRequest(titulo, descripcion, etiqueta, enlace, textoBoton,
                fechaInicio, fechaFin);
        return anuncioService.crear(sesion, datos, imagen);
    }

    @PatchMapping("/anuncios/{id}")
    public AnuncioAdminResponse cambiarVisibilidadAnuncio(UsuarioSesion sesion, @PathVariable("id") Integer idAnuncio,
                                                          @RequestParam("activo") boolean activo) {
        return anuncioService.cambiarVisibilidad(sesion, idAnuncio, activo);
    }

    @DeleteMapping("/anuncios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarAnuncio(UsuarioSesion sesion, @PathVariable("id") Integer idAnuncio) {
        anuncioService.eliminar(sesion, idAnuncio);
    }
}
