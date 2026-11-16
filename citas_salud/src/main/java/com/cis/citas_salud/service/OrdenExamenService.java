package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.OrdenExamen;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.OrdenExamenRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrdenExamenService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdenExamenRepository ordenExamenRepository;
    private final DoctorRepository doctorRepository;

    public OrdenExamenService(
            OrdenExamenRepository ordenExamenRepository,
            DoctorRepository doctorRepository) {
        this.ordenExamenRepository = ordenExamenRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public void crear(Atencion atencion, String examenesRequeridos, String motivoClinico) {
        String examenes = normalizarExamenes(examenesRequeridos);
        if (examenes.isBlank()) {
            throw ApiException.datosInvalidos("Agrega al menos un examen requerido.");
        }
        if (motivoClinico == null || motivoClinico.isBlank()) {
            throw ApiException.datosInvalidos("Escribe el motivo clínico de la orden de exámenes.");
        }

        OrdenExamen orden = new OrdenExamen();
        orden.setAtencion(atencion);
        orden.setExamenesRequeridos(examenes);
        orden.setMotivoClinico(motivoClinico.trim());
        ordenExamenRepository.save(orden);
    }

    @Transactional(readOnly = true)
    public CitaService.Archivo generarPdfDoctor(UsuarioSesion sesion, Integer idAtencion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);
        Doctor doctor = doctorRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró el perfil del doctor."));
        OrdenExamen orden = ordenExamenRepository.findByAtencion_IdAtencion(idAtencion)
                .filter(o -> o.getAtencion().getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa orden de exámenes."));
        return generarPdf(orden);
    }

    @Transactional(readOnly = true)
    public CitaService.Archivo generarPdfPaciente(UsuarioSesion sesion, Integer idCita) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);
        OrdenExamen orden = ordenExamenRepository.findByAtencion_Cita_IdCita(idCita)
                .filter(o -> o.getAtencion().getHistoria().getPaciente().getUsuario().getIdUsuario()
                        .equals(sesion.idUsuario()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa orden de exámenes."));
        return generarPdf(orden);
    }

    private CitaService.Archivo generarPdf(OrdenExamen orden) {
        Atencion atencion = orden.getAtencion();
        var paciente = atencion.getHistoria().getPaciente();
        var usuarioPaciente = paciente.getUsuario();
        Doctor doctor = atencion.getDoctor();
        var usuarioDoctor = doctor.getUsuario();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("P_NUMERO_ORDEN", "OE-" + String.format("%06d", orden.getIdOrdenExamen()));
        parametros.put("P_FECHA_EMISION", orden.getFechaEmision().format(FECHA));
        parametros.put("P_NOMBRE_PACIENTE", usuarioPaciente.getNombres() + " " + usuarioPaciente.getApellidos());
        parametros.put("P_DNI_PACIENTE", usuarioPaciente.getDni());
        parametros.put("P_HISTORIA_CLINICA", atencion.getHistoria().getNumeroHistoria());
        List<Map<String, ?>> secciones = List.of(
                seccion("Exámenes requeridos", orden.getExamenesRequeridos()),
                seccion("Motivo clínico", orden.getMotivoClinico()),
                seccion("Médico solicitante", usuarioDoctor.getNombres() + " " + usuarioDoctor.getApellidos()
                        + "\nEspecialidad: "
                        + (doctor.getEspecialidad() == null
                        ? "No registrada" : valor(doctor.getEspecialidad().getNombre()))
                        + "\nCMP: " + valor(doctor.getCmp()))
        );

        try {
            ClassPathResource plantilla = new ClassPathResource("reports/OrdenExamenes.jrxml");
            try (InputStream inputStream = plantilla.getInputStream()) {
                JasperReport reporte = JasperCompileManager.compileReport(inputStream);
                JasperPrint impresion = JasperFillManager.fillReport(
                        reporte, parametros, new JRMapCollectionDataSource(secciones));
                byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
                return new CitaService.Archivo(
                        "orden-examenes-" + orden.getIdOrdenExamen() + ".pdf", "application/pdf", pdf);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden de exámenes.", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró la plantilla de la orden de exámenes.", e);
        }
    }

    private String normalizarExamenes(String examenes) {
        if (examenes == null) {
            return "";
        }
        return examenes.lines()
                .map(String::trim)
                .filter(linea -> !linea.isBlank())
                .distinct()
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private Map<String, ?> seccion(String titulo, String contenido) {
        return Map.of("titulo", titulo, "contenido", contenido);
    }

    private String valor(String valor) {
        return valor == null || valor.isBlank() ? "No registrado" : valor;
    }
}
