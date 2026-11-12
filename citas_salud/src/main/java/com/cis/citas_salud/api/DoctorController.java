package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.DoctorDto.AtencionResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CambioFechaRequest;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import com.cis.citas_salud.api.dto.DoctorDto.DerivacionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.HorarioLibreResponse;
import com.cis.citas_salud.api.dto.DoctorDto.RegistrarAtencionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.SolicitudResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cis.citas_salud.service.RecetaMedicaService;
import com.cis.citas_salud.service.OrdenExamenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Panel del doctor (RF-14 a RF-16). */
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final RecetaMedicaService recetaMedicaService;
    private final OrdenExamenService ordenExamenService;

    public DoctorController(DoctorService doctorService, RecetaMedicaService recetaMedicaService,
                            OrdenExamenService ordenExamenService) {
        this.doctorService = doctorService;
        this.recetaMedicaService = recetaMedicaService;
        this.ordenExamenService = ordenExamenService;
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

    @PostMapping("/citas/{id}/atencion")
    @ResponseStatus(HttpStatus.CREATED)
    public AtencionResponse registrarAtencion(UsuarioSesion sesion, @PathVariable("id") Integer idCita,
                                              @Valid @RequestBody RegistrarAtencionRequest datos) {
        return doctorService.registrarAtencion(sesion, idCita, datos);
    }

    @GetMapping("/tipos-documento")
    public List<String> tiposDocumento() {
        return DoctorService.TIPOS_DOCUMENTO;
    }

    @PostMapping(value = "/atenciones/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoResponse subirDocumento(UsuarioSesion sesion, @PathVariable("id") Integer idAtencion,
                                            @RequestParam("tipoDocumento") String tipoDocumento,
                                            @RequestParam("archivo") MultipartFile archivo) {
        return doctorService.subirDocumento(sesion, idAtencion, tipoDocumento, archivo);
    }
    @GetMapping("/atenciones/{id}/receta")
    public ResponseEntity<byte[]> descargarReceta(
            UsuarioSesion sesion,
            @PathVariable("id") Integer idAtencion) {

        byte[] pdf = recetaMedicaService.generarReceta(sesion, idAtencion);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("receta-" + idAtencion + ".pdf", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }

    @GetMapping("/atenciones/{id}/orden-examenes")
    public ResponseEntity<byte[]> descargarOrdenExamen(
            UsuarioSesion sesion,
            @PathVariable("id") Integer idAtencion) {
        return CitaController.descarga(ordenExamenService.generarPdfDoctor(sesion, idAtencion));
    }
}
