package com.cis.citas_salud.service;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HistorialClinicoReportTest {

    @Test
    void compilesFillsAndExportsClinicalHistoryPdf() throws Exception {
        Map<String, Object> parametros = new HashMap<>(Map.of(
                "P_NOMBRE_PACIENTE", "Ana Quispe Rojas",
                "P_DNI", "12345678",
                "P_FECHA_NACIMIENTO", "01/01/1990",
                "P_SEXO", "F",
                "P_DIRECCION", "Chaclacayo",
                "P_GRUPO_SANGUINEO", "O+",
                "P_CONTACTO_EMERGENCIA", "Contacto de prueba",
                "P_TELEFONO_EMERGENCIA", "999999999",
                "P_NUMERO_HISTORIA", "HC-001",
                "P_ANTECEDENTES", "Antecedente de prueba"
        ));
        List<Map<String, ?>> filas = List.of(
                Map.of("linea", "CONSULTA: 11/11/2026 | Doctor de prueba | Medicina General"),
                Map.of("linea", "Diagnostico: Diagnostico de prueba")
        );

        try (InputStream input = new ClassPathResource("reports/HistorialClinico.jrxml").getInputStream()) {
            var report = JasperCompileManager.compileReport(input);
            var print = JasperFillManager.fillReport(
                    report, parametros, new JRMapCollectionDataSource(filas));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            assertTrue(pdf.length > 5);
            assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
        }
    }
}
