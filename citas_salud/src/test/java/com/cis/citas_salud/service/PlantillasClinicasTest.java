package com.cis.citas_salud.service;

import com.cis.citas_salud.api.dto.ReportePrescripcionDto.DiagnosticoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.MedicamentoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.TendenciaSemanal;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las plantillas deben estar en el formato de JasperReports 7 y tener un diseño válido.
 * No necesita base de datos: compilar la plantilla ya detecta un formato antiguo o una banda mal medida.
 */
class PlantillasClinicasTest {

    @ParameterizedTest
    @CsvSource({
            "FichaConsultaMedica, ID_ATENCION",
            "InformeMedico, ID_ATENCION",
            "InformeMedicoDerivacion, ID_ATENCION",
            "FichaConsultaPaciente, ID_PACIENTE",
            "ConstanciaCambioFecha, ID_SOLICITUD",
    })
    void laPlantillaCompilaYPideSuParametro(String plantilla, String parametro) {
        JasperReport reporte = GeneradorReportesSql.compilar(plantilla);

        assertEquals(plantilla, reporte.getName());
        assertNotNull(reporte.getQuery(), "la plantilla trae su consulta SQL");
        assertEquals("sql", reporte.getQuery().getLanguage());
        assertTrue(reporte.getQuery().getText().contains("$P{" + parametro + "}"));
        JRParameter declarado = Arrays.stream(reporte.getParameters())
                .filter(p -> p.getName().equals(parametro)).findFirst().orElseThrow();
        assertEquals(Integer.class, declarado.getValueClass());
    }

    @Test
    void constanciaMuestraElDoctorOriginalYEsperaDecisionDelPaciente() {
        String consulta = GeneradorReportesSql.compilar("ConstanciaCambioFecha").getQuery().getText();

        assertTrue(consulta.contains("s.json ->> 'doctorOriginal'"), consulta);
        assertTrue(consulta.contains("WHEN 'APROBADA' THEN 'Pendiente de decisión del paciente'"), consulta);
    }

    @Test
    void reportePrescripcionCompilaConSusIndicadoresYGraficos() {
        JasperReport reporte = GeneradorReportesSql.compilar("ReportePrescripcionClinica");

        assertEquals("ReportePrescripcionClinica", reporte.getName());
        assertTrue(Arrays.stream(reporte.getParameters()).anyMatch(p -> p.getName().equals("P_GRAFICO_MEDICAMENTOS")));
        assertTrue(Arrays.stream(reporte.getParameters()).anyMatch(p -> p.getName().equals("P_GRAFICO_DIAGNOSTICOS")));
        assertTrue(Arrays.stream(reporte.getParameters()).anyMatch(p -> p.getName().equals("P_GRAFICO_TENDENCIA")));
        assertTrue(Arrays.stream(reporte.getFields()).anyMatch(f -> f.getName().equals("doctor")));
    }

    @Test
    void reportePrescripcionSeLlenaYExportaConDatosDePrueba() {
        Map<String, Object> parametros = new java.util.HashMap<>();
        parametros.put("P_DESDE", "2026-10-01");
        parametros.put("P_HASTA", "2026-10-31");
        parametros.put("P_TOTAL_ATENCIONES", 10L);
        parametros.put("P_TOTAL_MEDICAMENTOS", 12L);
        parametros.put("P_PROMEDIO_MEDICAMENTOS", "1,5");
        parametros.put("P_PORCENTAJE_RECETA", "80 %");
        parametros.put("P_GRAFICO_MEDICAMENTOS", ReportePrescripcionGraficos.medicamentos(
                List.of(new MedicamentoFrecuente("Amoxicilina", 5))));
        parametros.put("P_GRAFICO_DIAGNOSTICOS", ReportePrescripcionGraficos.diagnosticos(
                List.of(new DiagnosticoFrecuente("Infección", 3))));
        parametros.put("P_GRAFICO_TENDENCIA", ReportePrescripcionGraficos.tendencia(
                List.of(new TendenciaSemanal("2026-10-05", 4))));
        var origen = new JRMapCollectionDataSource(List.of(Map.of(
                "doctor", "Dra. Ana Rojas",
                "especialidad", "Medicina General",
                "recetas", 8L)));
        GeneradorReportesSql generador = new GeneradorReportesSql(org.mockito.Mockito.mock(DataSource.class));

        byte[] pdf = generador.pdf("ReportePrescripcionClinica", parametros, origen);

        assertTrue(pdf.length > 4);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        byte[] sinMedicos = generador.pdf("ReportePrescripcionClinica", parametros,
                new JRMapCollectionDataSource(List.of()));
        assertTrue(sinMedicos.length > 4);
    }
}
