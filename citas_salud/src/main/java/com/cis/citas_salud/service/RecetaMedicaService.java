package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.RecetaMedicamentoDto;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.DetalleMedicamento;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.DoctorRepository;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecetaMedicaService {

    private final AtencionRepository atencionRepository;
    private final DoctorRepository doctorRepository;
    private final JdbcTemplate jdbcTemplate;

    public RecetaMedicaService(
            AtencionRepository atencionRepository,
            DoctorRepository doctorRepository,
            JdbcTemplate jdbcTemplate) {
        this.atencionRepository = atencionRepository;
        this.doctorRepository = doctorRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public byte[] generarReceta(UsuarioSesion sesion, Integer idAtencion) {

        // 1. Validar que el usuario sea doctor.
        sesion.exigirRol(UsuarioSesion.DOCTOR);

        // 2. Verificar que el doctor exista.
        var doctorSesion = doctorRepository
                .findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() ->
                        ApiException.noEncontrado("No se encontró el perfil del doctor."));

        // 3. Recuperar la atención.
        Atencion atencion = atencionRepository.findById(idAtencion)
                .orElseThrow(() ->
                        ApiException.noEncontrado("No se encontró la atención solicitada."));

        // 4. Verificar que la atención pertenezca al doctor autenticado.
        if (atencion.getDoctor() == null
                || !atencion.getDoctor().getIdDoctor()
                .equals(doctorSesion.getIdDoctor())) {
            throw ApiException.prohibido(
                    "No tienes permiso para descargar la receta de esta atención.");
        }
        return generarPdf(atencion, doctorSesion);
    }
    @Transactional(readOnly = true)
    public byte[] generarRecetaPaciente(Atencion atencion) {

        Doctor doctor = atencion.getDoctor();

        if (doctor == null) {
            throw ApiException.noEncontrado(
                    "No se encontró el médico de esta atención.");
        }

        return generarPdf(atencion, doctor);
    }

    private byte[] generarPdf(Atencion atencion, Doctor doctor) {
        // 5. Recuperar los datos del paciente.
        Paciente paciente = atencion.getHistoria().getPaciente();

        String nombrePaciente = paciente.getUsuario().getNombres()
                + " " + paciente.getUsuario().getApellidos();

        String nombreMedico = doctor.getUsuario().getNombres()
                + " " + doctor.getUsuario().getApellidos();

        String especialidad = doctor.getEspecialidad() != null
                ? doctor.getEspecialidad().getNombre()
                : "";

        // 6. Calcular la edad a la fecha de atención.
        Integer edad = null;

        if (paciente.getFechaNacimiento() != null) {
            edad = Period.between(paciente.getFechaNacimiento(),
                    atencion.getFechaAtencion().toLocalDate()).getYears();
        }

        // 7. Obtener el peso del último triaje anterior o igual a la atención.
        Double peso = obtenerPesoTriaje(
                atencion.getHistoria().getIdHistoria(),
                atencion.getFechaAtencion()
        );

        // 8. Convertir los medicamentos a JavaBeans compatibles con Jasper.
        List<RecetaMedicamentoDto> medicamentos = atencion.getMedicamentos()
                .stream()
                .map(this::convertirMedicamento)
                .toList();

        // 9. Preparar los parámetros del JRXML.
        Map<String, Object> parametros = new HashMap<>();

        parametros.put("P_CIUDAD", "");
        parametros.put("P_NOMBRE_MEDICO", nombreMedico);
        parametros.put("P_ESPECIALIDAD", especialidad);
        parametros.put("P_NUMERO_COLEGIATURA", doctor.getCmp());

        parametros.put("P_NOMBRE_PACIENTE", nombrePaciente);
        parametros.put("P_FECHA", Timestamp.valueOf(atencion.getFechaAtencion()));
        parametros.put("P_HISTORIA_CLINICA",
                atencion.getHistoria().getNumeroHistoria());

        parametros.put("P_EDAD", edad);
        parametros.put("P_PESO", peso);
        parametros.put("P_DIRECCION_PACIENTE", paciente.getDireccion());

        parametros.put("P_DIAGNOSTICO", atencion.getDiagnostico());

        // Estos datos todavía no existen en tus entidades o formulario.
        // Se dejan sin valor para no inventar información médica.
        parametros.put("P_CLAVE_ESTUPEFACIENTES", null);
        parametros.put("P_RENOVACIONES", null);
        parametros.put("P_NO_SUSTITUIR", null);

        parametros.put("P_OBSERVACIONES", atencion.getObservaciones());
        parametros.put("P_MEDICAMENTOS", medicamentos);

        // 10. Compilar, llenar y exportar el reporte.
        try {
            ClassPathResource recurso =
                    new ClassPathResource("reports/RecetaMedica.jrxml");

            try (InputStream inputStream = recurso.getInputStream()) {

                JasperReport reporte =
                        JasperCompileManager.compileReport(inputStream);

                JasperPrint impresion = JasperFillManager.fillReport(
                        reporte,
                        parametros,
                        new JREmptyDataSource(1)
                );

                return JasperExportManager.exportReportToPdf(impresion);
            }

        } catch (JRException e) {
            throw new IllegalStateException(
                    "No se pudo generar el PDF de la receta médica.", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "No se encontró el archivo JRXML de la receta médica.", e);
        }
    }

    private RecetaMedicamentoDto convertirMedicamento(
            DetalleMedicamento medicamento) {

        return new RecetaMedicamentoDto(
                medicamento.getNombreMedicamento(),
                medicamento.getDosis(),
                medicamento.getCantidad(),
                medicamento.getViaAdministracion(),
                medicamento.getPresentacion(),
                medicamento.getUnidadesPorToma(),
                medicamento.getFrecuencia(),
                medicamento.getDuracion()
        );
    }

    private Double obtenerPesoTriaje(
            Integer idHistoria,
            java.time.LocalDateTime fechaAtencion) {

        String sql = """
                SELECT peso
                FROM triaje
                WHERE id_historia = ?
                  AND fecha_triaje <= ?
                  AND peso IS NOT NULL
                ORDER BY fecha_triaje DESC, id_triaje DESC
                LIMIT 1
                """;

        List<BigDecimal> resultados = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getBigDecimal("peso"),
                idHistoria,
                Timestamp.valueOf(fechaAtencion)
        );

        return resultados.isEmpty()
                ? null
                : resultados.get(0).doubleValue();
    }
}