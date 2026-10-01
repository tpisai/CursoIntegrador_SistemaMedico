package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.CitaDto.AtencionHistorialResponse;
import com.cis.citas_salud.api.dto.CitaDto.CitaResponse;
import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.CitaDto.ReservarCitaRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.CitaService;
import com.cis.citas_salud.service.CitaService.Archivo;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Citas, historial y documentos del paciente (RF-06 a RF-11). */
@RestController
@RequestMapping("/api")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse reservar(UsuarioSesion sesion, @Valid @RequestBody ReservarCitaRequest datos) {
        return citaService.reservar(sesion, datos);
    }

    @GetMapping("/citas/mias")
    public List<CitaResponse> misCitas(UsuarioSesion sesion) {
        return citaService.misCitas(sesion);
    }

    @PatchMapping("/citas/{id}/cancelar")
    public CitaResponse cancelar(UsuarioSesion sesion, @PathVariable("id") Integer idCita) {
        return citaService.cancelar(sesion, idCita);
    }

    @GetMapping("/citas/{id}/ticket")
    public ResponseEntity<byte[]> ticket(UsuarioSesion sesion, @PathVariable("id") Integer idCita) {
        return descarga(citaService.ticket(sesion, idCita));
    }

    @GetMapping("/pacientes/me/historial")
    public List<AtencionHistorialResponse> historial(UsuarioSesion sesion) {
        return citaService.historial(sesion);
    }

    @GetMapping("/pacientes/me/documentos")
    public List<DocumentoResponse> documentos(UsuarioSesion sesion) {
        return citaService.documentos(sesion);
    }

    @GetMapping("/documentos/{id}/descarga")
    public ResponseEntity<byte[]> documento(UsuarioSesion sesion, @PathVariable("id") Integer idDocumento) {
        return descarga(citaService.documento(sesion, idDocumento));
    }

    static ResponseEntity<byte[]> descarga(Archivo archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }
}
