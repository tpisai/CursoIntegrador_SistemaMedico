package com.cis.citas_salud.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DoctorDto {

    private DoctorDto() {
    }

    public record CitaAsignadaResponse(
            Integer idCita,
            String numeroTicket,
            String estado,
            String fecha,
            String horaInicio,
            String horaFin,
            // "Luis Ramírez H."
            String paciente,
            String dniPaciente,
            String motivo,
            String consultorio) {
    }

    public record HorarioLibreResponse(Integer idHorario, String fecha, String horaInicio, String consultorio) {
    }

    public record CambioFechaRequest(
            @NotNull(message = "Elige la cita que quieres cambiar.") Integer idCita,
            @NotNull(message = "Elige la nueva fecha propuesta.") Integer idHorarioNuevo,
            @NotBlank(message = "Escribe el motivo del cambio.") @Size(max = 500, message = "El motivo es demasiado largo.") String motivo) {
    }

    public record DerivacionRequest(
            @NotNull(message = "Elige al paciente.") Integer idCita,
            @NotBlank(message = "Elige el establecimiento de destino.") @Size(max = 150) String establecimiento,
            @NotBlank(message = "Elige la especialidad requerida.") @Size(max = 100) String especialidad,
            @NotBlank(message = "Escribe el motivo de la derivación.") @Size(max = 500, message = "El motivo es demasiado largo.") String motivo) {
    }

    public record SolicitudResponse(
            Integer idSolicitud,
            String tipo,
            String estado,
            String fechaSolicitud,
            String paciente,
            String fechaCita,
            String horaCita,
            // Resumen legible: nueva fecha propuesta o establecimiento de destino.
            String detalle,
            String motivo,
            String respuestaAdmin) {
    }

    // RF-10: lo que el doctor registra al atender (tabla atencion).
    public record RegistrarAtencionRequest(
            @NotNull(message = "Indica si el paciente asistió.") Boolean asistio,
            @Size(max = 2000, message = "La anamnesis es demasiado larga.") String anamnesis,
            @Size(max = 2000, message = "El examen físico es demasiado largo.") String examenFisico,
            @Size(max = 2000, message = "El diagnóstico es demasiado largo.") String diagnostico,
            @Size(max = 2000, message = "El tratamiento es demasiado largo.") String tratamiento,
            @Size(max = 2000, message = "Las observaciones son demasiado largas.") String observaciones) {
    }

    public record AtencionResponse(
            Integer idCita,
            // ATENDIDA o NO_ASISTIO
            String estadoCita,
            // null si el paciente no asistió (no se crea atención).
            Integer idAtencion) {
    }
}
