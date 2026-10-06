package com.cis.citas_salud.api.dto;

/** Campañas que ve el paciente (pantalla 05 del prototipo). */
public final class CampaniaDto {

    private CampaniaDto() {
    }

    public record CampaniaResponse(
            Integer idCampania,
            String titulo,
            String descripcion,
            String fechaInicio,
            String fechaFin,
            // "HH:mm" o null si no tiene hora fija.
            String hora,
            String lugar,
            String estado,
            int cupos,
            int cuposDisponibles,
            // Inscripción del paciente en sesión; ambos null si no está inscrito.
            Integer idInscripcion,
            String codigoInscripcion) {
    }

    public record InscripcionResponse(
            Integer idInscripcion,
            // Código del comprobante, p. ej. "C-000012".
            String codigo,
            Integer idCampania,
            String campania,
            String fechaInscripcion) {
    }
}
