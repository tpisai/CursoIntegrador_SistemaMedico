package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.AnuncioDto.AnuncioResponse;
import com.cis.citas_salud.service.AnuncioService;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** Carrusel de eventos: público, igual que el catálogo de especialidades. */
@RestController
@RequestMapping("/api/anuncios")
public class AnuncioController {

    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @GetMapping
    public List<AnuncioResponse> vigentes() {
        return anuncioService.vigentes();
    }

    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable("id") Integer idAnuncio) {
        Archivo archivo = anuncioService.imagen(idAnuncio);
        // La URL lleva ?v=, así que la imagen de un id no cambia: el navegador la guarda un día.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(archivo.contenido());
    }
}
