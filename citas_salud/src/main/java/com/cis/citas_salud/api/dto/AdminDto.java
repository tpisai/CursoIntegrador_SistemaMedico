package com.cis.citas_salud.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Panel de administración y recepción (pantalla 04 del prototipo). */
public final class AdminDto {

    private AdminDto() {
    }

    public record ResumenResponse(
            long citasHoy,
            long solicitudesPendientes,
            long campaniasVigentes,
            // % de citas del mes en curso a las que el paciente no asistió.
            int ausentismoMes) {
    }

    public record SolicitudAdminResponse(
            Integer idSolicitud,
            String tipo,
            String estado,
            String fechaSolicitud,
            String doctor,
            String especialidad,
            String paciente,
            String dniPaciente,
            String fechaCita,
            String horaCita,
            // Nueva fecha propuesta o establecimiento de destino, ya en texto.
            String detalle,
            String motivo,
            String respuestaAdmin) {
    }

    public enum Decision { APROBAR, RECHAZAR }

    public record ResponderSolicitudRequest(
            @NotNull(message = "Elige si apruebas o rechazas la solicitud.") Decision decision,
            @Size(max = 500, message = "La respuesta es demasiado larga.") String respuesta) {
    }

    // RF-13: el personal define los turnos de un doctor para un día.
    public record CrearHorariosRequest(
            @NotNull(message = "Elige al doctor.") Integer idDoctor,
            @NotNull(message = "Elige el consultorio.") Integer idConsultorio,
            @NotNull(message = "Elige la fecha.") LocalDate fecha,
            @NotNull(message = "Indica la hora de inicio.") LocalTime horaInicio,
            @NotNull(message = "Indica la hora de fin.") LocalTime horaFin,
            @NotNull(message = "Elige la duración de cada turno.")
            @Min(value = 10, message = "Cada turno dura al menos 10 minutos.")
            @Max(value = 120, message = "Cada turno dura como máximo 120 minutos.") Integer duracionMinutos) {
    }

    public record CrearHorariosResponse(
            int creados,
            // Turnos que se cruzaban con otros ya existentes de ese doctor.
            int omitidos,
            String fecha,
            String doctor,
            String consultorio) {
    }

    // RF-20: nueva campaña médica.
    public record CrearCampaniaRequest(
            @NotBlank(message = "Escribe el título de la campaña.") @Size(max = 150, message = "El título es demasiado largo.") String titulo,
            @Size(max = 2000, message = "La descripción es demasiado larga.") String descripcion,
            @NotNull(message = "Indica la fecha de inicio.") LocalDate fechaInicio,
            @NotNull(message = "Indica la fecha de fin.") LocalDate fechaFin,
            LocalTime hora,
            @Size(max = 255, message = "El lugar es demasiado largo.") String lugar,
            @NotNull(message = "Indica la cantidad de cupos.")
            @Min(value = 1, message = "La campaña necesita al menos 1 cupo.")
            @Max(value = 10000, message = "El máximo es 10 000 cupos.") Integer cupos) {
    }

    public record CampaniaAdminResponse(
            Integer idCampania,
            String titulo,
            String descripcion,
            String fechaInicio,
            String fechaFin,
            String hora,
            String lugar,
            String estado,
            int cupos,
            int cuposDisponibles,
            long inscritos) {
    }
}
