package com.cis.citas_salud.api.dto;

import java.time.LocalDate;

/** Carrusel de eventos y anuncios del portal del paciente. */
public final class AnuncioDto {

    private AnuncioDto() {
    }

    /**
     * Datos del formulario de administración. Llegan como campos de un multipart junto con la
     * imagen; AnuncioService los valida.
     */
    public record CrearAnuncioRequest(
            String titulo,
            String descripcion,
            String etiqueta,
            String enlace,
            String textoBoton,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
    }

    /** Lo que ve el paciente en el carrusel. */
    public record AnuncioResponse(
            Integer idAnuncio,
            String titulo,
            String descripcion,
            String etiqueta,
            String enlace,
            String textoBoton,
            // Ruta bajo /api para el img, p. ej. "/anuncios/4/imagen?v=1732540800".
            String imagenUrl) {
    }

    /** Fila del listado de administración. */
    public record AnuncioAdminResponse(
            Integer idAnuncio,
            String titulo,
            String descripcion,
            String etiqueta,
            String enlace,
            String textoBoton,
            String fechaInicio,
            String fechaFin,
            boolean activo,
            // VIGENTE, PROGRAMADO, VENCIDO u OCULTO.
            String estado,
            String imagenUrl,
            int anchoImagen,
            int altoImagen,
            String fechaCreacion) {
    }
}
