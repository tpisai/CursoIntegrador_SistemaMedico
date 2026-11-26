package com.cis.citas_salud.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compila y exporta plantillas JasperReports, usando la conexión de la aplicación para plantillas SQL o
 * una fuente de datos preparada por el servicio llamador.
 */
@Component
public class GeneradorReportesSql {

    private final DataSource dataSource;
    // Compilar una plantilla tarda; se hace una vez y se reutiliza.
    private final Map<String, JasperReport> compilados = new ConcurrentHashMap<>();

    public GeneradorReportesSql(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** PDF de la plantilla {@code reports/<plantilla>.jrxml} con los parámetros de su consulta. */
    public byte[] pdf(String plantilla, Map<String, Object> parametros) {
        JasperReport reporte = compilados.computeIfAbsent(plantilla, GeneradorReportesSql::compilar);
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setReadOnly(true);
            // JasperReports agrega sus propios parámetros al mapa: necesita uno modificable.
            JasperPrint impresion = JasperFillManager.fillReport(reporte, new HashMap<>(parametros), conexion);
            return JasperExportManager.exportReportToPdf(impresion);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo conectar a la base de datos para generar el reporte.", e);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte " + plantilla + ".", e);
        }
    }

    /** PDF de la plantilla Jasper con una fuente de datos preparada por el servicio llamador. */
    public byte[] pdf(String plantilla, Map<String, Object> parametros, JRDataSource origen) {
        JasperReport reporte = compilados.computeIfAbsent(plantilla, GeneradorReportesSql::compilar);
        try {
            JasperPrint impresion = JasperFillManager.fillReport(reporte, new HashMap<>(parametros), origen);
            return JasperExportManager.exportReportToPdf(impresion);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte " + plantilla + ".", e);
        }
    }

    static JasperReport compilar(String plantilla) {
        try (InputStream entrada = new ClassPathResource("reports/" + plantilla + ".jrxml").getInputStream()) {
            return JasperCompileManager.compileReport(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró la plantilla " + plantilla + ".", e);
        } catch (JRException e) {
            throw new IllegalStateException("La plantilla " + plantilla + " no es válida.", e);
        }
    }
}
