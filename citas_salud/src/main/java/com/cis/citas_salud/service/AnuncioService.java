package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AnuncioDto.AnuncioAdminResponse;
import com.cis.citas_salud.api.dto.AnuncioDto.AnuncioResponse;
import com.cis.citas_salud.api.dto.AnuncioDto.CrearAnuncioRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Anuncio;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.repository.AnuncioRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Carrusel de eventos del portal del paciente: administración sube banners rectangulares
 * (recomendado 1200 x 640 px) con su vigencia y el paciente los ve en Campañas y en su inicio.
 */
@Service
@Transactional
public class AnuncioService {

    /** Tamaño recomendado del banner (proporción 15:8). */
    public static final int ANCHO_RECOMENDADO = 1200;
    public static final int ALTO_RECOMENDADO = 640;
    /** Medidas mínimas aceptadas: por debajo el banner se ve borroso en pantallas grandes. */
    static final int ANCHO_MINIMO = 960;
    static final int ALTO_MINIMO = 512;
    /** Margen sobre la proporción 15:8 (6 %): 1200 x 680 pasa, una imagen cuadrada no. */
    static final double TOLERANCIA_PROPORCION = 0.06;

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png");

    private final AnuncioRepository anuncioRepository;
    private final AdminService adminService;
    private final Path carpetaArchivos;

    public AnuncioService(AnuncioRepository anuncioRepository, AdminService adminService,
                          @Value("${saludgrau.archivos.carpeta:archivos}") String carpetaArchivos) {
        this.anuncioRepository = anuncioRepository;
        this.adminService = adminService;
        this.carpetaArchivos = Path.of(carpetaArchivos).toAbsolutePath().normalize();
    }

    // ----- Paciente (y cualquier visitante) -----

    @Transactional(readOnly = true)
    public List<AnuncioResponse> vigentes() {
        return anuncioRepository.vigentes(LocalDate.now()).stream()
                .map(a -> new AnuncioResponse(a.getIdAnuncio(), a.getTitulo(), a.getDescripcion(), a.getEtiqueta(),
                        a.getEnlace(), a.getTextoBoton(), imagenUrl(a)))
                .toList();
    }

    /** Imagen pública del banner: la etiqueta img no puede enviar el token de la sesión. */
    @Transactional(readOnly = true)
    public Archivo imagen(Integer idAnuncio) {
        Anuncio anuncio = buscar(idAnuncio);
        Path ruta = carpetaArchivos.resolve(anuncio.getRutaImagen()).normalize();
        if (!ruta.startsWith(carpetaArchivos) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("La imagen del anuncio ya no está disponible.");
        }
        try {
            return new Archivo(ruta.getFileName().toString(), anuncio.getTipoMime(), Files.readAllBytes(ruta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen del anuncio", e);
        }
    }

    // ----- Administración -----

    @Transactional(readOnly = true)
    public List<AnuncioAdminResponse> listarParaAdmin(UsuarioSesion sesion) {
        adminService.personal(sesion);
        LocalDate hoy = LocalDate.now();
        return anuncioRepository.findAllByOrderByFechaCreacionDescIdAnuncioDesc().stream()
                .map(a -> respuestaAdmin(a, hoy))
                .toList();
    }

    public AnuncioAdminResponse crear(UsuarioSesion sesion, CrearAnuncioRequest datos, MultipartFile imagen) {
        PersonalAdministrativo admin = adminService.personal(sesion);
        LocalDate hoy = LocalDate.now();

        String titulo = textoOpcional(datos.titulo());
        if (titulo == null || titulo.length() < 5) {
            throw ApiException.datosInvalidos("Escribe un título de al menos 5 caracteres.");
        }
        if (titulo.length() > 120) throw ApiException.datosInvalidos("El título admite hasta 120 caracteres.");
        String descripcion = limitar(datos.descripcion(), 300, "La descripción admite hasta 300 caracteres.");
        String etiqueta = limitar(datos.etiqueta(), 40, "La etiqueta admite hasta 40 caracteres.");
        String textoBoton = limitar(datos.textoBoton(), 40, "El texto del botón admite hasta 40 caracteres.");
        String enlace = enlaceSeguro(datos.enlace());
        if (datos.fechaInicio() == null || datos.fechaFin() == null) {
            throw ApiException.datosInvalidos("Indica desde y hasta cuándo se muestra el anuncio.");
        }
        if (datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw ApiException.datosInvalidos("La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (datos.fechaFin().isBefore(hoy)) {
            throw ApiException.datosInvalidos("El anuncio no puede terminar en el pasado.");
        }

        if (imagen == null || imagen.isEmpty()) throw ApiException.datosInvalidos("Adjunta la imagen del banner.");
        String extension = imagen.getContentType() == null ? null : EXTENSIONES.get(imagen.getContentType());
        if (extension == null) throw ApiException.datosInvalidos("La imagen debe ser JPG o PNG.");
        byte[] contenido;
        try {
            contenido = imagen.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen", e);
        }
        int[] medidas = medidas(contenido);
        validarMedidas(medidas[0], medidas[1]);

        String ruta = "anuncios/" + System.currentTimeMillis() + "." + extension;
        try {
            Path destino = carpetaArchivos.resolve(ruta).normalize();
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen del anuncio", e);
        }

        Anuncio anuncio = new Anuncio();
        anuncio.setPersonal(admin);
        anuncio.setTitulo(titulo);
        anuncio.setDescripcion(descripcion);
        anuncio.setEtiqueta(etiqueta);
        anuncio.setEnlace(enlace);
        anuncio.setTextoBoton(enlace == null ? null : textoBoton != null ? textoBoton : "Más información");
        anuncio.setFechaInicio(datos.fechaInicio());
        anuncio.setFechaFin(datos.fechaFin());
        anuncio.setRutaImagen(ruta);
        anuncio.setTipoMime(imagen.getContentType());
        anuncio.setAnchoImagen(medidas[0]);
        anuncio.setAltoImagen(medidas[1]);
        anuncioRepository.save(anuncio);
        return respuestaAdmin(anuncio, hoy);
    }

    /** Muestra u oculta el banner sin borrarlo. */
    public AnuncioAdminResponse cambiarVisibilidad(UsuarioSesion sesion, Integer idAnuncio, boolean activo) {
        adminService.personal(sesion);
        Anuncio anuncio = buscar(idAnuncio);
        anuncio.setActivo(activo);
        anuncioRepository.save(anuncio);
        return respuestaAdmin(anuncio, LocalDate.now());
    }

    public void eliminar(UsuarioSesion sesion, Integer idAnuncio) {
        adminService.personal(sesion);
        Anuncio anuncio = buscar(idAnuncio);
        anuncioRepository.delete(anuncio);
        try {
            Path ruta = carpetaArchivos.resolve(anuncio.getRutaImagen()).normalize();
            if (ruta.startsWith(carpetaArchivos)) Files.deleteIfExists(ruta);
        } catch (IOException e) {
            // El registro ya no existe; un archivo huérfano no afecta al portal.
        }
    }

    /** Ancho y alto en píxeles; falla si el archivo no es una imagen legible. */
    static int[] medidas(byte[] contenido) {
        BufferedImage imagen;
        try {
            imagen = ImageIO.read(new ByteArrayInputStream(contenido));
        } catch (IOException e) {
            imagen = null;
        }
        if (imagen == null) throw ApiException.datosInvalidos("No pudimos leer la imagen. Prueba con otro JPG o PNG.");
        return new int[]{imagen.getWidth(), imagen.getHeight()};
    }

    /** Rectangular 15:8 y al menos 960 x 512 px; lo ideal es 1200 x 640 px. */
    static void validarMedidas(int ancho, int alto) {
        String recomendado = "Usa una imagen rectangular de " + ANCHO_RECOMENDADO + " × " + ALTO_RECOMENDADO
                + " px (proporción 15:8). La tuya mide " + ancho + " × " + alto + " px.";
        if (ancho < ANCHO_MINIMO || alto < ALTO_MINIMO) {
            throw ApiException.datosInvalidos("La imagen es muy pequeña. " + recomendado);
        }
        double esperada = (double) ANCHO_RECOMENDADO / ALTO_RECOMENDADO;
        double proporcion = (double) ancho / alto;
        if (Math.abs(proporcion - esperada) / esperada > TOLERANCIA_PROPORCION) {
            throw ApiException.datosInvalidos("La imagen no tiene la proporción del carrusel. " + recomendado);
        }
    }

    /** Solo rutas del portal ("/paciente/agendar") o direcciones http(s); evita "javascript:". */
    static String enlaceSeguro(String enlace) {
        String limpio = textoOpcional(enlace);
        if (limpio == null) return null;
        if (limpio.length() > 255) throw ApiException.datosInvalidos("El enlace admite hasta 255 caracteres.");
        boolean interno = limpio.startsWith("/") && !limpio.startsWith("//");
        boolean externo = limpio.startsWith("https://") || limpio.startsWith("http://");
        if (!interno && !externo) {
            throw ApiException.datosInvalidos("El enlace debe empezar con / (página del portal) o con https://.");
        }
        return limpio;
    }

    static String estado(Anuncio a, LocalDate hoy) {
        if (!a.isActivo()) return "OCULTO";
        if (hoy.isBefore(a.getFechaInicio())) return "PROGRAMADO";
        if (hoy.isAfter(a.getFechaFin())) return "VENCIDO";
        return "VIGENTE";
    }

    private Anuncio buscar(Integer idAnuncio) {
        return anuncioRepository.findById(idAnuncio)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese anuncio."));
    }

    /** La versión en la URL evita que el navegador muestre una imagen vieja guardada en caché. */
    private static String imagenUrl(Anuncio a) {
        long version = a.getFechaCreacion() != null ? a.getFechaCreacion().toEpochSecond(ZoneOffset.UTC) : 0;
        return "/anuncios/" + a.getIdAnuncio() + "/imagen?v=" + version;
    }

    private static AnuncioAdminResponse respuestaAdmin(Anuncio a, LocalDate hoy) {
        return new AnuncioAdminResponse(
                a.getIdAnuncio(),
                a.getTitulo(),
                a.getDescripcion(),
                a.getEtiqueta(),
                a.getEnlace(),
                a.getTextoBoton(),
                Formato.fecha(a.getFechaInicio()),
                Formato.fecha(a.getFechaFin()),
                a.isActivo(),
                estado(a, hoy),
                imagenUrl(a),
                a.getAnchoImagen(),
                a.getAltoImagen(),
                a.getFechaCreacion() != null ? Formato.fechaHora(a.getFechaCreacion()) : null);
    }

    private static String limitar(String texto, int maximo, String mensaje) {
        String limpio = textoOpcional(texto);
        if (limpio != null && limpio.length() > maximo) throw ApiException.datosInvalidos(mensaje);
        return limpio;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
