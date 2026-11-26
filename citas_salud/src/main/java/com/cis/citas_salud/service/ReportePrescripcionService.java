package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.DiagnosticoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.Kpis;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.MedicamentoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.PrescripcionPorMedico;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.ReporteResponse;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.TendenciaSemanal;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.repository.ReportePrescripcionRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportePrescripcionService {

    private static final int MAX_DIAS = 366;
    private static final String PLANTILLA = "ReportePrescripcionClinica";

    private final AdminService adminService;
    private final ReportePrescripcionRepository repository;
    private final GeneradorReportesSql generadorReportesSql;

    public ReportePrescripcionService(AdminService adminService, ReportePrescripcionRepository repository,
                                      GeneradorReportesSql generadorReportesSql) {
        this.adminService = adminService;
        this.repository = repository;
        this.generadorReportesSql = generadorReportesSql;
    }

    public ReporteResponse reporte(UsuarioSesion sesion, LocalDate desde, LocalDate hasta,
                                   Integer idEspecialidad, Integer idDoctor) {
        adminService.personal(sesion);
        validarPeriodo(desde, hasta);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        List<Object[]> filasResumen = repository.resumen(inicio, fin, idEspecialidad, idDoctor);
        if (filasResumen.isEmpty() || filasResumen.get(0).length < 3) {
            throw new IllegalStateException("La consulta de indicadores del reporte devolvió un resultado incompleto.");
        }
        Object[] resumen = filasResumen.get(0);
        long totalAtenciones = numero(resumen[0]);
        long totalMedicamentos = numero(resumen[1]);
        long recetas = numero(resumen[2]);
        Kpis kpis = new Kpis(
                totalAtenciones,
                totalMedicamentos,
                recetas == 0 ? 0 : redondear((double) totalMedicamentos / recetas, 2),
                totalAtenciones == 0 ? 0 : redondear(100.0 * recetas / totalAtenciones, 1));

        List<MedicamentoFrecuente> medicamentos = repository.topMedicamentos(inicio, fin, idEspecialidad, idDoctor)
                .stream().map(fila -> new MedicamentoFrecuente((String) fila[0], numero(fila[1]))).toList();
        List<DiagnosticoFrecuente> diagnosticos = repository.topDiagnosticos(inicio, fin, idEspecialidad, idDoctor)
                .stream().map(fila -> new DiagnosticoFrecuente((String) fila[0], numero(fila[1]))).toList();
        List<PrescripcionPorMedico> medicos = repository.porMedico(inicio, fin, idEspecialidad, idDoctor)
                .stream().map(fila -> new PrescripcionPorMedico(
                        ((Number) fila[0]).intValue(), (String) fila[1], (String) fila[2], numero(fila[3]))).toList();
        List<TendenciaSemanal> tendencia = tendencia(desde, hasta,
                repository.tendenciaSemanal(inicio, fin, idEspecialidad, idDoctor));

        return new ReporteResponse(Formato.fecha(desde), Formato.fecha(hasta), kpis,
                medicamentos, diagnosticos, medicos, tendencia);
    }

    public Archivo pdf(UsuarioSesion sesion, LocalDate desde, LocalDate hasta,
                       Integer idEspecialidad, Integer idDoctor) {
        ReporteResponse reporte = reporte(sesion, desde, hasta, idEspecialidad, idDoctor);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("P_DESDE", reporte.desde());
        parametros.put("P_HASTA", reporte.hasta());
        parametros.put("P_TOTAL_ATENCIONES", reporte.kpis().totalAtenciones());
        parametros.put("P_TOTAL_MEDICAMENTOS", reporte.kpis().totalMedicamentos());
        parametros.put("P_PROMEDIO_MEDICAMENTOS", formatoNumero(reporte.kpis().promedioMedicamentosPorReceta()));
        parametros.put("P_PORCENTAJE_RECETA", formatoNumero(reporte.kpis().porcentajeConsultasConReceta()) + " %");
        parametros.put("P_GRAFICO_MEDICAMENTOS", ReportePrescripcionGraficos.medicamentos(reporte.topMedicamentos()));
        parametros.put("P_GRAFICO_DIAGNOSTICOS", ReportePrescripcionGraficos.diagnosticos(reporte.topDiagnosticos()));
        parametros.put("P_GRAFICO_TENDENCIA", ReportePrescripcionGraficos.tendencia(reporte.tendenciaSemanal()));
        byte[] contenido = generadorReportesSql.pdf(
                PLANTILLA, parametros, new JRMapCollectionDataSource(mapasMedicos(reporte.porMedico())));
        return new Archivo("reporte-integral-morbilidad-prescripcion-" + desde + "-a-" + hasta + ".pdf",
                "application/pdf", contenido);
    }

    public Archivo csv(UsuarioSesion sesion, LocalDate desde, LocalDate hasta,
                       Integer idEspecialidad, Integer idDoctor) {
        ReporteResponse reporte = reporte(sesion, desde, hasta, idEspecialidad, idDoctor);
        StringBuilder csv = new StringBuilder("\uFEFF");
        agregarFila(csv, "Reporte Integral de Morbilidad y Prescripción Médica");
        agregarFila(csv, "Desde", reporte.desde(), "Hasta", reporte.hasta());
        agregarFila(csv, "");
        agregarFila(csv, "Indicador", "Valor");
        agregarFila(csv, "Total de atenciones", reporte.kpis().totalAtenciones());
        agregarFila(csv, "Total de medicamentos recetados", reporte.kpis().totalMedicamentos());
        agregarFila(csv, "Promedio de medicamentos por receta", reporte.kpis().promedioMedicamentosPorReceta());
        agregarFila(csv, "Consultas con receta (%)", reporte.kpis().porcentajeConsultasConReceta());
        agregarFila(csv, "");
        agregarFila(csv, "Top medicamentos", "Recetas");
        reporte.topMedicamentos().forEach(fila -> agregarFila(csv, fila.nombre(), fila.cantidad()));
        agregarFila(csv, "");
        agregarFila(csv, "Diagnósticos frecuentes", "Atenciones");
        reporte.topDiagnosticos().forEach(fila -> agregarFila(csv, fila.nombre(), fila.atenciones()));
        agregarFila(csv, "");
        agregarFila(csv, "Médico", "Especialidad", "Recetas");
        reporte.porMedico().forEach(fila -> agregarFila(csv, fila.doctor(), fila.especialidad(), fila.recetas()));
        agregarFila(csv, "");
        agregarFila(csv, "Semana (lunes)", "Recetas");
        reporte.tendenciaSemanal().forEach(fila -> agregarFila(csv, fila.semana(), fila.recetas()));
        return new Archivo("reporte-integral-morbilidad-prescripcion-" + desde + "-a-" + hasta + ".csv",
                "text/csv;charset=UTF-8", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw ApiException.datosInvalidos("Debes indicar las fechas inicial y final del reporte.");
        }
        if (hasta.isBefore(desde)) {
            throw ApiException.datosInvalidos("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_DIAS) {
            throw ApiException.datosInvalidos("El reporte abarca como máximo un año.");
        }
    }

    private static List<TendenciaSemanal> tendencia(LocalDate desde, LocalDate hasta, List<Object[]> filas) {
        Map<LocalDate, Long> valores = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            valores.put(fecha(fila[0]), numero(fila[1]));
        }
        LocalDate semana = desde.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate ultimaSemana = hasta.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TendenciaSemanal> resultado = new ArrayList<>();
        while (!semana.isAfter(ultimaSemana)) {
            resultado.add(new TendenciaSemanal(Formato.fecha(semana), valores.getOrDefault(semana, 0L)));
            semana = semana.plusWeeks(1);
        }
        return List.copyOf(resultado);
    }

    private static LocalDate fecha(Object valor) {
        if (valor instanceof LocalDate fecha) return fecha;
        if (valor instanceof Date fechaSql) return fechaSql.toLocalDate();
        return LocalDate.parse(valor.toString());
    }

    private static long numero(Object valor) {
        if (!(valor instanceof Number cantidad)) {
            throw new IllegalStateException("La consulta del reporte devolvió un valor numérico inválido.");
        }
        return cantidad.longValue();
    }

    private static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }

    private static String formatoNumero(double valor) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE")).format(valor);
    }

    private static List<Map<String, ?>> mapasMedicos(List<PrescripcionPorMedico> filas) {
        return filas.stream().<Map<String, ?>>map(fila -> {
            Map<String, Object> datos = new HashMap<>();
            datos.put("doctor", fila.doctor());
            datos.put("especialidad", fila.especialidad());
            datos.put("recetas", fila.recetas());
            return datos;
        }).toList();
    }

    private static void agregarFila(StringBuilder csv, Object... valores) {
        for (int indice = 0; indice < valores.length; indice++) {
            if (indice > 0) csv.append(';');
            csv.append('"').append(String.valueOf(valores[indice]).replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }
}
