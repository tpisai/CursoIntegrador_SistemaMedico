package com.cis.citas_salud.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Fechas como "YYYY-MM-DD" y horas como "HH:mm" (ver Formato).
public final class CitaDto {

    private CitaDto() {
    }

    public record EspecialidadResponse(Integer idEspecialidad, String nombre) {
    }

    public record DoctorResponse(Integer idDoctor, Integer idEspecialidad, String nombreCompleto) {
    }

    public record ConsultorioResponse(Integer idConsultorio, String nombre, String zona, String piso, String numero) {
    }

    public record DisponibilidadResponse(String fecha, long cuposDisponibles) {
    }

    public record HorarioResponse(Integer idHorario, String fecha, String horaInicio, String horaFin, String estado) {
    }

    public record ReservarCitaRequest(
            @NotNull(message = "Elige un horario.") Integer idHorario,
            @Size(max = 500, message = "El motivo es demasiado largo.") String motivo) {
    }

    public record CitaResponse(
            Integer idCita,
            String numeroTicket,
            String estado,
            String fecha,
            String horaInicio,
            String horaFin,
            String especialidad,
            String doctor,
            String consultorio) {
    }

    public record AtencionHistorialResponse(Integer idCita, String fecha, String especialidad, String doctor, String estado) {
    }

    public record DocumentoResponse(Integer idDocumento, String tipoDocumento, String nombreArchivo, String fechaCarga) {
    }
}
