package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.RecetaMedicamentoDto;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.DetalleMedicamento;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.OrdenExamen;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.OrdenExamenRepository;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecetaMedicaService {

    public static class ActividadLaboratorioItem {
        private final String descripcionActividad;

        public ActividadLaboratorioItem(String descripcionActividad) {
            this.descripcionActividad = descripcionActividad;
        }

        public String getDescripcionActividad() {
            return descripcionActividad;
        }
    }

    private final AtencionRepository atencionRepository;
    private final DoctorRepository doctorRepository;
    private final OrdenExamenRepository ordenExamenRepository;
    private final JdbcTemplate jdbcTemplate;

    public RecetaMedicaService(
            AtencionRepository atencionRepository,
            DoctorRepository doctorRepository,
            OrdenExamenRepository ordenExamenRepository,
            JdbcTemplate jdbcTemplate) {
        this.atencionRepository = atencionRepository;
        this.doctorRepository = doctorRepository;
        this.ordenExamenRepository = ordenExamenRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public byte[] generarReceta(UsuarioSesion sesion, Integer idAtencion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);

        var doctorSesion = doctorRepository
                .findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() ->
                        ApiException.noEncontrado("No se encontr\u00F3 el perfil del doctor."));

        Atencion atencion = atencionRepository.findById(idAtencion)
                .orElseThrow(() ->
                        ApiException.noEncontrado("No se encontr\u00F3 la atenci\u00F3n solicitada."));

        if (atencion.getDoctor() == null
                || !atencion.getDoctor().getIdDoctor().equals(doctorSesion.getIdDoctor())) {
            throw ApiException.prohibido(
                    "No tienes permiso para descargar la receta de esta atenci\u00F3n.");
        }
        return generarPdf(atencion, doctorSesion);
    }

    @Transactional(readOnly = true)
    public byte[] generarRecetaPaciente(Atencion atencion) {
        Doctor doctor = atencion.getDoctor();
        if (doctor == null) {
            throw ApiException.noEncontrado("No se encontr\u00F3 el m\u00E9dico de esta atenci\u00F3n.");
        }
        return generarPdf(atencion, doctor);
    }

    // =========================================================================
    // REPORTES PARA EL DOCTOR: CONSTANCIA, CERTIFICADO E INFORME LABORATORIO
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] generarConstanciaAtencion(UsuarioSesion sesion, Integer idAtencion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);

        var doctorSesion = doctorRepository
                .findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontr\u00F3 el perfil del doctor."));

        Atencion atencion = atencionRepository.findById(idAtencion)
                .orElseThrow(() -> ApiException.noEncontrado("No se encontr\u00F3 la atenci\u00F3n solicitada."));

        if (atencion.getDoctor() == null || !atencion.getDoctor().getIdDoctor().equals(doctorSesion.getIdDoctor())) {
            throw ApiException.prohibido("No tienes permiso para generar la constancia de esta atenci\u00F3n.");
        }

        return generarConstancia(atencion, atencion.getDoctor());
    }

    @Transactional(readOnly = true)
    public byte[] generarCertificadoMedico(UsuarioSesion sesion, Integer idAtencion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);

        var doctorSesion = doctorRepository
                .findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontr\u00F3 el perfil del doctor."));

        Atencion atencion = atencionRepository.findById(idAtencion)
                .orElseThrow(() -> ApiException.noEncontrado("No se encontr\u00F3 la atenci\u00F3n solicitada."));

        if (atencion.getDoctor() == null || !atencion.getDoctor().getIdDoctor().equals(doctorSesion.getIdDoctor())) {
            throw ApiException.prohibido("No tienes permiso para generar el certificado de esta atenci\u00F3n.");
        }

        return generarCertificado(atencion, atencion.getDoctor());
    }

    @Transactional(readOnly = true)
    public byte[] generarInformeLaboratorio(UsuarioSesion sesion, int anio, int mes) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);

        var doctorSesion = doctorRepository
                .findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontr\u00F3 el perfil del doctor."));

        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        List<OrdenExamen> ordenes = ordenExamenRepository.findByAtencion_Cita_Horario_FechaBetween(inicio, fin);

        List<ActividadLaboratorioItem> items = ordenes.stream()
                .filter(o -> o.getExamenesRequeridos() != null)
                .flatMap(o -> Arrays.stream(o.getExamenesRequeridos().split("\\r?\\n")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ActividadLaboratorioItem(e.getKey() + " (" + e.getValue() + " solicitudes registradas)."))
                .toList();

        if (items.isEmpty()) {
            items = List.of(new ActividadLaboratorioItem("Recepci\u00F3n y verificaci\u00F3n de solicitudes de laboratorio en el periodo."));
        }

        Locale esPeru = Locale.forLanguageTag("es-PE");
        String nombreMes = inicio.getMonth().getDisplayName(TextStyle.FULL, esPeru).toUpperCase(esPeru);
        String fechaActualFormateada = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd 'DE' MMMM 'DEL' yyyy", esPeru))
                .toUpperCase(esPeru);
        String nombreDoctor = doctorSesion.getUsuario().getNombres() + " " + doctorSesion.getUsuario().getApellidos();

        Map<String, Object> params = new HashMap<>();
        params.put("ANIO_OFICIAL", "\"A\u00D1O DE LA ESPERANZA Y EL FORTALECIMIENTO DE LA DEMOCRACIA\"");
        params.put("NUMERO_INFORME", String.format("INFORME N\u00B0%03d-LAB-CSMG-%d", mes, anio));
        params.put("DESTINATARIO_NOMBRE", "JEFATURA DE LABORATORIO CL\u00CDNICO");
        params.put("DESTINATARIO_CARGO", "JEFE DE LABORATORIO \u00B7 C.S. MIGUEL GRAU");
        params.put("REMITENTE_NOMBRE", nombreDoctor.toUpperCase(esPeru));
        params.put("REMITENTE_CARGO", "M\u00E9dico / Responsable de \u00C1rea \u00B7 CMP " + doctorSesion.getCmp());
        params.put("ASUNTO", "ACTIVIDADES Y EX\u00C1MENES REALIZADOS DURANTE EL MES DE " + nombreMes);
        params.put("FECHA_DOCUMENTO", "CHACLACAYO, " + fechaActualFormateada);
        params.put("PERIODO_TEXTO", nombreMes.toLowerCase(esPeru) + " de " + anio);
        params.put("RESUMEN_CIERRE", "Se adjunta el consolidado de " + ordenes.size()
                + " \u00F3rdenes de laboratorio registradas en la base de datos durante el periodo indicado.");

        try {
            ClassPathResource recurso = new ClassPathResource("reports/InformeLaboratorio.jrxml");
            try (InputStream inputStream = recurso.getInputStream()) {
                JasperReport reporte = JasperCompileManager.compileReport(inputStream);
                JasperPrint impresion = JasperFillManager.fillReport(
                        reporte, params, new JRBeanCollectionDataSource(items));
                return JasperExportManager.exportReportToPdf(impresion);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo compilar el reporte InformeLaboratorio.jrxml", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se encontr\u00F3 el archivo reports/InformeLaboratorio.jrxml", e);
        }
    }

    // =========================================================================
    // REPORTES PARA EL PACIENTE: CONSTANCIA DE ATENCI\u00D3N Y CERTIFICADO M\u00C9DICO
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] generarConstanciaPaciente(Atencion atencion) {
        Doctor doctor = atencion.getDoctor();
        if (doctor == null) {
            throw ApiException.noEncontrado("No se encontr\u00F3 el m\u00E9dico de esta atenci\u00F3n.");
        }
        return generarConstancia(atencion, doctor);
    }

    @Transactional(readOnly = true)
    public byte[] generarCertificadoPaciente(Atencion atencion) {
        Doctor doctor = atencion.getDoctor();
        if (doctor == null) {
            throw ApiException.noEncontrado("No se encontr\u00F3 el m\u00E9dico de esta atenci\u00F3n.");
        }
        return generarCertificado(atencion, doctor);
    }

    // =========================================================================
    // M\u00C9TODOS INTERNOS DE COMPILACI\u00D3N JASPERREPORTS
    // =========================================================================

    private byte[] generarConstancia(Atencion atencion, Doctor doctor) {
        Paciente paciente = atencion.getHistoria().getPaciente();

        String nombrePaciente = paciente.getUsuario().getNombres() + " " + paciente.getUsuario().getApellidos();
        String nombreMedico = doctor.getUsuario().getNombres() + " " + doctor.getUsuario().getApellidos();
        String especialidad = doctor.getEspecialidad() != null ? doctor.getEspecialidad().getNombre() : "Medicina General";

        Map<String, Object> params = new HashMap<>();
        params.put("P_NOMBRE_MEDICO", nombreMedico);
        params.put("P_ESPECIALIDAD", especialidad);
        params.put("P_NUMERO_COLEGIATURA", doctor.getCmp());
        params.put("P_NOMBRE_PACIENTE", nombrePaciente);
        params.put("P_DNI_PACIENTE", paciente.getUsuario().getDni());
        params.put("P_HISTORIA_CLINICA", atencion.getHistoria().getNumeroHistoria());
        params.put("P_FECHA", atencion.getFechaAtencion() != null ? atencion.getFechaAtencion().toLocalDate().toString() : "");
        params.put("P_HORA", atencion.getFechaAtencion() != null ? atencion.getFechaAtencion().toLocalTime().toString() : "");
        params.put("P_MOTIVO", atencion.getDiagnostico());

        return compilarYGenerarPdf("reports/ConstanciaAtencion.jrxml", params);
    }

    private byte[] generarCertificado(Atencion atencion, Doctor doctor) {
        Paciente paciente = atencion.getHistoria().getPaciente();

        String nombrePaciente = paciente.getUsuario().getNombres() + " " + paciente.getUsuario().getApellidos();
        String nombreMedico = doctor.getUsuario().getNombres() + " " + doctor.getUsuario().getApellidos();
        String especialidad = doctor.getEspecialidad() != null ? doctor.getEspecialidad().getNombre() : "Medicina General";

        Integer edad = null;
        if (paciente.getFechaNacimiento() != null && atencion.getFechaAtencion() != null) {
            edad = Period.between(paciente.getFechaNacimiento(), atencion.getFechaAtencion().toLocalDate()).getYears();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("P_NOMBRE_MEDICO", nombreMedico);
        params.put("P_ESPECIALIDAD", especialidad);
        params.put("P_NUMERO_COLEGIATURA", doctor.getCmp());
        params.put("P_NOMBRE_PACIENTE", nombrePaciente);
        params.put("P_DNI_PACIENTE", paciente.getUsuario().getDni());
        params.put("P_HISTORIA_CLINICA", atencion.getHistoria().getNumeroHistoria());
        params.put("P_EDAD", edad);
        params.put("P_FECHA", atencion.getFechaAtencion() != null ? atencion.getFechaAtencion().toLocalDate().toString() : "");
        params.put("P_DIAGNOSTICO", atencion.getDiagnostico());
        params.put("P_DIAS_DESCANSO", 2);
        params.put("P_INDICACIONES", atencion.getObservaciones());

        return compilarYGenerarPdf("reports/CertificadoMedico.jrxml", params);
    }

    private byte[] compilarYGenerarPdf(String rutaReporte, Map<String, Object> parametros) {
        try {
            ClassPathResource recurso = new ClassPathResource(rutaReporte);
            try (InputStream inputStream = recurso.getInputStream()) {
                JasperReport reporte = JasperCompileManager.compileReport(inputStream);
                JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource(1));
                return JasperExportManager.exportReportToPdf(impresion);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo compilar el reporte Jasper: " + rutaReporte, e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se encontr\u00F3 el archivo del reporte: " + rutaReporte, e);
        }
    }

    private byte[] generarPdf(Atencion atencion, Doctor doctor) {
        Paciente paciente = atencion.getHistoria().getPaciente();

        String nombrePaciente = paciente.getUsuario().getNombres()
                + " " + paciente.getUsuario().getApellidos();

        String nombreMedico = doctor.getUsuario().getNombres()
                + " " + doctor.getUsuario().getApellidos();

        String especialidad = doctor.getEspecialidad() != null
                ? doctor.getEspecialidad().getNombre()
                : "";

        Integer edad = null;
        if (paciente.getFechaNacimiento() != null && atencion.getFechaAtencion() != null) {
            edad = Period.between(paciente.getFechaNacimiento(),
                    atencion.getFechaAtencion().toLocalDate()).getYears();
        }

        Double peso = obtenerPesoTriaje(
                atencion.getHistoria().getIdHistoria(),
                atencion.getFechaAtencion()
        );

        List<RecetaMedicamentoDto> medicamentos = atencion.getMedicamentos() != null
                ? atencion.getMedicamentos().stream().map(this::convertirMedicamento).toList()
                : Collections.emptyList();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("P_CIUDAD", "");
        parametros.put("P_NOMBRE_MEDICO", nombreMedico);
        parametros.put("P_ESPECIALIDAD", especialidad);
        parametros.put("P_NUMERO_COLEGIATURA", doctor.getCmp());
        parametros.put("P_NOMBRE_PACIENTE", nombrePaciente);
        parametros.put("P_FECHA", atencion.getFechaAtencion() != null ? Timestamp.valueOf(atencion.getFechaAtencion()) : null);
        parametros.put("P_HISTORIA_CLINICA", atencion.getHistoria().getNumeroHistoria());
        parametros.put("P_EDAD", edad);
        parametros.put("P_PESO", peso);
        parametros.put("P_DIRECCION_PACIENTE", paciente.getDireccion());
        parametros.put("P_DIAGNOSTICO", atencion.getDiagnostico());
        parametros.put("P_CLAVE_ESTUPEFACIENTES", null);
        parametros.put("P_RENOVACIONES", null);
        parametros.put("P_NO_SUSTITUIR", null);
        parametros.put("P_OBSERVACIONES", atencion.getObservaciones());
        parametros.put("P_MEDICAMENTOS", medicamentos);

        try {
            ClassPathResource recurso = new ClassPathResource("reports/RecetaMedica.jrxml");
            try (InputStream inputStream = recurso.getInputStream()) {
                JasperReport reporte = JasperCompileManager.compileReport(inputStream);
                JasperPrint jasperPrint = JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource(1));
                return JasperExportManager.exportReportToPdf(jasperPrint);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la receta m\u00E9dica", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se encontr\u00F3 el archivo del reporte de la receta m\u00E9dica", e);
        }
    }

    private Double obtenerPesoTriaje(Integer idHistoria, LocalDateTime fechaAtencion) {
        try {
            String sql = "SELECT t.peso FROM triaje t "
                    + "WHERE t.id_historia = ? AND t.fecha_triaje <= ? "
                    + "ORDER BY t.fecha_triaje DESC LIMIT 1";
            List<BigDecimal> resultados = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> rs.getBigDecimal("peso"),
                    idHistoria,
                    fechaAtencion
            );
            if (!resultados.isEmpty() && resultados.get(0) != null) {
                return resultados.get(0).doubleValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private RecetaMedicamentoDto convertirMedicamento(DetalleMedicamento detalle) {
        String nombre = "";
        try {
            java.lang.reflect.Method m = detalle.getClass().getMethod("getNombre");
            Object val = m.invoke(detalle);
            if (val != null) nombre = val.toString();
        } catch (Exception e1) {
            try {
                java.lang.reflect.Method m = detalle.getClass().getMethod("getMedicamento");
                Object val = m.invoke(detalle);
                if (val != null) nombre = val.toString();
            } catch (Exception ignored) {}
        }

        String indicaciones = "";
        try {
            java.lang.reflect.Method m = detalle.getClass().getMethod("getIndicaciones");
            Object val = m.invoke(detalle);
            if (val != null) indicaciones = val.toString();
        } catch (Exception e2) {
            try {
                java.lang.reflect.Method m = detalle.getClass().getMethod("getIndicacionesEspeciales");
                Object val = m.invoke(detalle);
                if (val != null) indicaciones = val.toString();
            } catch (Exception ignored) {}
        }

        return new RecetaMedicamentoDto(
                nombre,
                detalle.getDosis() != null ? detalle.getDosis() : "",
                detalle.getFrecuencia() != null ? detalle.getFrecuencia() : "",
                detalle.getDuracion() != null ? detalle.getDuracion() : "",
                detalle.getViaAdministracion() != null ? detalle.getViaAdministracion() : "",
                indicaciones,
                "",
                ""
        );
    }
}