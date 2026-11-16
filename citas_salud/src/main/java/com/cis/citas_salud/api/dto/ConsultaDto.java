package com.cis.citas_salud.api.dto;

import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Página de consulta del doctor: paciente, historia clínica y resultados de exámenes. */
public final class ConsultaDto {

    private ConsultaDto() {
    }

    public record PacienteResponse(
            Integer idPaciente,
            String nombreCompleto,
            String dni,
            String fechaNacimiento,
            // null si no se registró la fecha de nacimiento.
            Integer edad,
            String sexo,
            String grupoSanguineo,
            String telefono,
            String correo,
            String direccion,
            String contactoEmergencia,
            String telefonoEmergencia,
            String numeroHistoria,
            String alergias,
            String antecedentes,
            String medicacionHabitual) {
    }

    public record MedicamentoResponse(
            String nombreMedicamento,
            String dosis,
            String presentacion,
            String viaAdministracion,
            String unidadesPorToma,
            String frecuencia,
            String duracion,
            String cantidad) {
    }

    public record ResultadoExamenResponse(
            String tipoExamen,
            String resultado,
            String valorReferencia,
            String interpretacion) {
    }

    public record AtencionPreviaResponse(
            Integer idAtencion,
            Integer idCita,
            String fecha,
            String especialidad,
            // CONSULTA o APOYO_DIAGNOSTICO
            String tipoEspecialidad,
            String doctor,
            // La registró el doctor que consulta: solo él descarga su receta y sus documentos clínicos.
            boolean propia,
            // La cita tiene una solicitud de derivación: se puede emitir la referencia.
            boolean tieneDerivacion,
            String anamnesis,
            String examenFisico,
            String diagnostico,
            String tratamiento,
            String observaciones,
            List<MedicamentoResponse> medicamentos,
            List<ResultadoExamenResponse> resultados,
            List<DocumentoResponse> documentos) {
    }

    public record ConsultaResponse(
            CitaAsignadaResponse cita,
            String especialidad,
            // CONSULTA: ficha de consulta. APOYO_DIAGNOSTICO: reporte de exámenes.
            String modo,
            // La cita está activa y es de hoy o de un día anterior.
            boolean puedeAtender,
            // Atención ya registrada para esta cita; null si aún no se atiende.
            Integer idAtencion,
            PacienteResponse paciente,
            List<AtencionPreviaResponse> historial) {
    }

    public record ResultadoExamenRequest(
            @NotBlank(message = "Indica el tipo de examen.")
            @Size(max = 150, message = "El tipo de examen es demasiado largo.") String tipoExamen,
            @NotBlank(message = "Escribe el resultado de cada examen.")
            @Size(max = 300, message = "El resultado es demasiado largo.") String resultado,
            @Size(max = 150, message = "El valor de referencia es demasiado largo.") String valorReferencia,
            @Size(max = 1000, message = "La interpretación es demasiado larga.") String interpretacion) {
    }

    public record ReporteExamenesRequest(
            @NotEmpty(message = "Agrega al menos un examen.")
            @Size(max = 30, message = "Registra como máximo 30 exámenes por atención.")
            List<@Valid ResultadoExamenRequest> resultados) {
    }

    public record AntecedentesRequest(
            @Size(max = 1000, message = "Las alergias son demasiado largas.") String alergias,
            @Size(max = 2000, message = "Los antecedentes son demasiado largos.") String antecedentes,
            @Size(max = 1000, message = "La medicación habitual es demasiado larga.") String medicacionHabitual) {
    }
}
