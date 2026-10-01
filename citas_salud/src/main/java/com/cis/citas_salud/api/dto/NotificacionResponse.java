package com.cis.citas_salud.api.dto;

public record NotificacionResponse(
        Integer idNotificacion,
        // CITA, SOLICITUD, CAMPANIA, SISTEMA o RECORDATORIO
        String tipo,
        String titulo,
        String mensaje,
        boolean leida,
        // "YYYY-MM-DDTHH:mm:ss"
        String fechaEnvio) {
}
