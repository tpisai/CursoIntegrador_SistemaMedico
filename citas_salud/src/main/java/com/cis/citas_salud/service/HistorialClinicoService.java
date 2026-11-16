package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.DetalleMedicamento;
import com.cis.citas_salud.entity.HistoriaClinica;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.PacienteRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistorialClinicoService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final AtencionRepository atencionRepository;

    public HistorialClinicoService(
            PacienteRepository pacienteRepository,
            HistoriaClinicaRepository historiaClinicaRepository,
            AtencionRepository atencionRepository) {
        this.pacienteRepository = pacienteRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.atencionRepository = atencionRepository;
    }

    @Transactional(readOnly = true)
    public CitaService.Archivo generarPdf(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);

        Paciente paciente = pacienteRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró el perfil del paciente."));
        HistoriaClinica historia = historiaClinicaRepository.findByPaciente_IdPaciente(paciente.getIdPaciente())
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró la historia clínica del paciente."));
        List<Atencion> atenciones = atencionRepository
                .findByHistoria_IdHistoriaOrderByFechaAtencionDesc(historia.getIdHistoria());

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("P_NOMBRE_PACIENTE",
                paciente.getUsuario().getNombres() + " " + paciente.getUsuario().getApellidos());
        parametros.put("P_DNI", paciente.getUsuario().getDni());
        parametros.put("P_FECHA_NACIMIENTO", fecha(paciente.getFechaNacimiento()));
        parametros.put("P_SEXO", texto(paciente.getSexo()));
        parametros.put("P_DIRECCION", texto(paciente.getDireccion()));
        parametros.put("P_GRUPO_SANGUINEO", texto(paciente.getGrupoSanguineo()));
        parametros.put("P_CONTACTO_EMERGENCIA", texto(paciente.getContactoEmergencia()));
        parametros.put("P_TELEFONO_EMERGENCIA", texto(paciente.getTelefonoEmergencia()));
        parametros.put("P_NUMERO_HISTORIA", historia.getNumeroHistoria());
        parametros.put("P_ANTECEDENTES", texto(historia.getObservaciones()));

        List<Map<String, ?>> filas = new ArrayList<>();
        for (Atencion atencion : atenciones) {
            filas.addAll(filasAtencion(atencion));
        }
        if (filas.isEmpty()) {
            filas.add(Map.of("linea", "No hay consultas registradas en esta historia clinica."));
        }

        try {
            ClassPathResource plantilla = new ClassPathResource("reports/HistorialClinico.jrxml");
            try (InputStream inputStream = plantilla.getInputStream()) {
                JasperReport reporte = JasperCompileManager.compileReport(inputStream);
                JasperPrint impresion = JasperFillManager.fillReport(
                        reporte, parametros, new JRMapCollectionDataSource(filas));
                byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
                return new CitaService.Archivo("historial-clinico.pdf", "application/pdf", pdf);
            }
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el PDF del historial clínico.", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró la plantilla del historial clínico.", e);
        }
    }

    private List<Map<String, Object>> filasAtencion(Atencion atencion) {
        String medico = atencion.getDoctor().getUsuario().getNombres()
                + " " + atencion.getDoctor().getUsuario().getApellidos();
        String especialidad = atencion.getDoctor().getEspecialidad() == null
                ? "No registrada" : texto(atencion.getDoctor().getEspecialidad().getNombre());
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila("CONSULTA: " + fechaHora(atencion) + " | " + medico + " | " + especialidad));
        filas.add(fila("Motivo de consulta: " + texto(atencion.getCita().getMotivo())));
        filas.add(fila("Anamnesis: " + texto(atencion.getAnamnesis())));
        filas.add(fila("Examen fisico: " + texto(atencion.getExamenFisico())));
        filas.add(fila("Diagnostico: " + texto(atencion.getDiagnostico())));
        filas.add(fila("Tratamiento: " + texto(atencion.getTratamiento())));
        filas.add(fila("Medicamentos: " + medicamentos(atencion.getMedicamentos())));
        filas.add(fila("Observaciones del medico: " + texto(atencion.getObservaciones())));
        filas.add(fila(""));
        return filas;
    }

    private Map<String, Object> fila(String contenido) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("linea", contenido);
        return fila;
    }

    private String fechaHora(Atencion atencion) {
        return atencion.getFechaAtencion() == null
                ? "Fecha no registrada" : atencion.getFechaAtencion().format(FECHA_HORA);
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? "No registrado" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String medicamentos(List<DetalleMedicamento> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return "Sin medicamentos registrados.";
        }
        return medicamentos.stream()
                .map(medicamento -> {
                    List<String> indicaciones = new ArrayList<>();
                    agregar(indicaciones, medicamento.getDosis());
                    agregar(indicaciones, medicamento.getViaAdministracion());
                    agregar(indicaciones, medicamento.getFrecuencia());
                    agregar(indicaciones, medicamento.getDuracion());
                    agregar(indicaciones, medicamento.getCantidad());
                    return medicamento.getNombreMedicamento()
                            + (indicaciones.isEmpty() ? "" : " (" + String.join("; ", indicaciones) + ")");
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Sin medicamentos registrados.");
    }

    private void agregar(List<String> valores, String valor) {
        if (valor != null && !valor.isBlank()) {
            valores.add(valor.trim());
        }
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "No registrado" : valor.toString();
    }
}
