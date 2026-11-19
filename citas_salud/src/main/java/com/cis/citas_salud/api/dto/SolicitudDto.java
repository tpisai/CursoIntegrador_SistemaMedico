package com.cis.citas_salud.api.dto;

import jakarta.validation.constraints.NotNull;

/** Portal del paciente: resolver un cambio de fecha aprobado y descargar su constancia (RF-17). */
public final class SolicitudDto {

    private SolicitudDto() {
    }

    /** Lo que el paciente decide cuando administración aprueba el cambio de fecha. */
    public enum DecisionPaciente {
        MISMO_DOCTOR,
        OTRO_DOCTOR,
        CANCELADA
    }

    public record ResolverCambioRequest(
            @NotNull(message = "Elige qué hacer con tu cita.") DecisionPaciente decision,
            // Obligatorio para MISMO_DOCTOR y OTRO_DOCTOR: el turno elegido en el calendario.
            Integer idHorarioNuevo) {
    }

    /** Cambio de fecha aprobado que todavía espera la decisión del paciente. */
    public record CambioPendienteResponse(
            Integer idSolicitud,
            Integer idCita,
            Integer idDoctor,
            Integer idEspecialidad,
            // Consultorio de la cita original: el paciente puede mantenerlo al elegir otro horario.
            Integer idConsultorio,
            String doctor,
            String especialidad,
            String fecha,
            String hora,
            String consultorio,
            String motivo,
            String fechaSolicitud,
            String respuestaAdmin) {
    }

    /** Fila del historial de cambios de fecha del paciente. */
    public record CambioPacienteResponse(
            Integer idSolicitud,
            String estado,
            String fechaSolicitud,
            String fechaCita,
            String horaCita,
            // "Cambio al …" o "Cambio solicitado: …"
            String detalle,
            String motivo,
            String respuestaAdmin,
            // La constancia solo existe cuando administración aprobó la solicitud.
            boolean constanciaDisponible) {
    }

    public record ResolucionResponse(
            Integer idSolicitud,
            String estado,
            String decision,
            // Mensaje listo para mostrar al paciente.
            String mensaje,
            // Cita nueva cuando eligió otro doctor.
            Integer idCitaNueva,
            String numeroTicket) {
    }
}
