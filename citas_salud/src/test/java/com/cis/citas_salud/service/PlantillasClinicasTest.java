package com.cis.citas_salud.service;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las plantillas con consulta SQL deben estar en el formato de JasperReports 7 y tener un diseño válido.
 * No necesita base de datos: compilar la plantilla ya detecta un formato antiguo o una banda mal medida.
 */
class PlantillasClinicasTest {

    @ParameterizedTest
    @CsvSource({
            "FichaConsultaMedica, ID_ATENCION",
            "InformeMedico, ID_ATENCION",
            "InformeMedicoDerivacion, ID_ATENCION",
            "FichaConsultaPaciente, ID_PACIENTE",
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
}
