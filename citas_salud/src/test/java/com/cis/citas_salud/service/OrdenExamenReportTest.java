package com.cis.citas_salud.service;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenExamenReportTest {

    @Test
    void compilesFillsAndExportsExaminationOrderPdf() throws Exception {
        Map<String, Object> parametros = new HashMap<>(Map.of(
                "P_NUMERO_ORDEN", "OE-000001",
                "P_FECHA_EMISION", "11/11/2026 10:30",
                "P_NOMBRE_PACIENTE", "Ana Quispe Rojas",
                "P_DNI_PACIENTE", "12345678",
                "P_HISTORIA_CLINICA", "HC-000001"
        ));
        List<Map<String, ?>> secciones = List.of(
                Map.of("titulo", "Exámenes requeridos", "contenido", "Hemograma completo\nGlucosa en ayunas"),
                Map.of("titulo", "Motivo clínico", "contenido", "Evaluación clínica de prueba"),
                Map.of("titulo", "Médico solicitante", "contenido",
                        "Rodrigo Mendoza\nEspecialidad: Medicina General\nCMP: 045812")
        );

        try (InputStream input = new ClassPathResource("reports/OrdenExamenes.jrxml").getInputStream()) {
            var report = JasperCompileManager.compileReport(input);
            var print = JasperFillManager.fillReport(report, parametros, new JRMapCollectionDataSource(secciones));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            assertTrue(pdf.length > 5);
            assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
        }
    }
}
