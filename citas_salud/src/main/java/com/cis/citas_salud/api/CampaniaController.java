package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.CampaniaDto.CampaniaResponse;
import com.cis.citas_salud.api.dto.CampaniaDto.InscripcionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.CampaniaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Campañas médicas para el paciente (RF-19, RF-21). */
@RestController
@RequestMapping("/api/campanias")
public class CampaniaController {

    private final CampaniaService campaniaService;

    public CampaniaController(CampaniaService campaniaService) {
        this.campaniaService = campaniaService;
    }

    @GetMapping
    public List<CampaniaResponse> vigentes(UsuarioSesion sesion) {
        return campaniaService.vigentes(sesion);
    }

    @PostMapping("/{id}/inscripcion")
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionResponse inscribir(UsuarioSesion sesion, @PathVariable("id") Integer idCampania) {
        return campaniaService.inscribir(sesion, idCampania);
    }

    @GetMapping("/inscripciones/{id}/comprobante")
    public ResponseEntity<byte[]> comprobante(UsuarioSesion sesion, @PathVariable("id") Integer idInscripcion) {
        return CitaController.descarga(campaniaService.comprobante(sesion, idInscripcion));
    }
}
