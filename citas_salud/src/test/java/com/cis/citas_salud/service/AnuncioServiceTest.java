package com.cis.citas_salud.service;

import com.cis.citas_salud.api.dto.AnuncioDto.AnuncioAdminResponse;
import com.cis.citas_salud.api.dto.AnuncioDto.CrearAnuncioRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Anuncio;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.repository.AnuncioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Carrusel de eventos: banners de 1200 x 640 px que administración publica para los pacientes. */
@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private AnuncioRepository anuncioRepository;
    @Mock
    private AdminService adminService;
    @TempDir
    Path carpeta;

    private AnuncioService servicio;
    private final UsuarioSesion sesionAdmin = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);

    @BeforeEach
    void preparar() {
        servicio = new AnuncioService(anuncioRepository, adminService, carpeta.toString());
    }

    private static MockMultipartFile png(int ancho, int alto) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), "png", salida);
        return new MockMultipartFile("imagen", "banner.png", "image/png", salida.toByteArray());
    }

    private static CrearAnuncioRequest datos(String enlace) {
        return new CrearAnuncioRequest("Nueva especialidad de Cardiología", "Atención desde diciembre.",
                "Nueva especialidad", enlace, null, HOY, HOY.plusDays(30));
    }

    @Test
    void crearGuardaElBannerConSusMedidas() throws IOException {
        when(adminService.personal(sesionAdmin)).thenReturn(new PersonalAdministrativo());

        AnuncioAdminResponse respuesta = servicio.crear(sesionAdmin, datos("/paciente/agendar"), png(1200, 640));

        ArgumentCaptor<Anuncio> guardado = ArgumentCaptor.forClass(Anuncio.class);
        verify(anuncioRepository).save(guardado.capture());
        Anuncio anuncio = guardado.getValue();
        assertEquals(1200, anuncio.getAnchoImagen());
        assertEquals(640, anuncio.getAltoImagen());
        assertEquals("Más información", anuncio.getTextoBoton());
        assertTrue(Files.isRegularFile(carpeta.resolve(anuncio.getRutaImagen())));
        assertEquals("VIGENTE", respuesta.estado());
    }

    @Test
    void sinEnlaceNoGuardaTextoDeBoton() throws IOException {
        when(adminService.personal(sesionAdmin)).thenReturn(new PersonalAdministrativo());

        servicio.crear(sesionAdmin, datos(null), png(1200, 640));

        ArgumentCaptor<Anuncio> guardado = ArgumentCaptor.forClass(Anuncio.class);
        verify(anuncioRepository).save(guardado.capture());
        assertNull(guardado.getValue().getTextoBoton());
    }

    @Test
    void rechazaUnaImagenCuadrada() throws IOException {
        when(adminService.personal(sesionAdmin)).thenReturn(new PersonalAdministrativo());

        var error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(sesionAdmin, datos(null), png(1000, 1000)));

        assertTrue(error.getMessage().contains("1200 × 640"));
        verify(anuncioRepository, never()).save(any());
    }

    @Test
    void rechazaUnaImagenPequenia() {
        exigeError(HttpStatus.BAD_REQUEST, () -> AnuncioService.validarMedidas(600, 320));
    }

    @Test
    void aceptaUnaImagenMasGrandeConLaMismaProporcion() {
        AnuncioService.validarMedidas(2400, 1280);
        AnuncioService.validarMedidas(1200, 675);
    }

    @Test
    void rechazaEnlacesQueNoSonDelPortalNiWeb() {
        exigeError(HttpStatus.BAD_REQUEST, () -> AnuncioService.enlaceSeguro("javascript:alert(1)"));
        exigeError(HttpStatus.BAD_REQUEST, () -> AnuncioService.enlaceSeguro("//otro-sitio.com"));
        assertEquals("/paciente/agendar", AnuncioService.enlaceSeguro(" /paciente/agendar "));
        assertEquals("https://www.gob.pe/minsa", AnuncioService.enlaceSeguro("https://www.gob.pe/minsa"));
        assertNull(AnuncioService.enlaceSeguro("  "));
    }

    @Test
    void rechazaUnAnuncioQueYaTermino() throws IOException {
        when(adminService.personal(sesionAdmin)).thenReturn(new PersonalAdministrativo());
        CrearAnuncioRequest vencido = new CrearAnuncioRequest("Feria de salud 2025", null, null, null, null,
                HOY.minusDays(10), HOY.minusDays(1));

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.crear(sesionAdmin, vencido, png(1200, 640)));
    }

    @Test
    void ocultarLoSacaDelCarruselSinBorrarlo() {
        Anuncio anuncio = new Anuncio();
        anuncio.setIdAnuncio(4);
        anuncio.setFechaInicio(HOY.minusDays(1));
        anuncio.setFechaFin(HOY.plusDays(1));
        anuncio.setAnchoImagen(1200);
        anuncio.setAltoImagen(640);
        when(anuncioRepository.findById(4)).thenReturn(Optional.of(anuncio));

        AnuncioAdminResponse respuesta = servicio.cambiarVisibilidad(sesionAdmin, 4, false);

        assertFalse(anuncio.isActivo());
        assertEquals("OCULTO", respuesta.estado());
        verify(anuncioRepository, never()).delete(any());
    }

    @Test
    void estadoSegunLaVigencia() {
        Anuncio anuncio = new Anuncio();
        anuncio.setFechaInicio(HOY.plusDays(2));
        anuncio.setFechaFin(HOY.plusDays(9));
        assertEquals("PROGRAMADO", AnuncioService.estado(anuncio, HOY));
        assertEquals("VIGENTE", AnuncioService.estado(anuncio, HOY.plusDays(5)));
        assertEquals("VENCIDO", AnuncioService.estado(anuncio, HOY.plusDays(10)));
    }
}
