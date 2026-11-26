package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.service.CitaService.Archivo;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.repository.ReporteIntegralCitasRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/** Reporte PDF nuevo e independiente: consultas agregadas sobre citas y solicitudes. */
@Service
@Transactional(readOnly = true)
public class ReporteIntegralCitasService {

    private static final int MAX_DIAS = 366;
    private static final Set<String> SECCIONES = Set.of("RESUMEN", "ESPECIALIDAD", "DOCTORES", "CAMBIOS", "DERIVACIONES");
    private static final Font TITULO = new Font(Font.HELVETICA, 17, Font.BOLD, new Color(28, 58, 74));
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(28, 58, 74));
    private static final Font TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font CABECERA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Color[] COLORES_GRAFICO = {
            new Color(37, 99, 235), new Color(239, 68, 68), new Color(16, 185, 129),
            new Color(245, 158, 11), new Color(139, 92, 246), new Color(6, 182, 212),
            new Color(236, 72, 153), new Color(132, 204, 22)
    };
    private static final float ANCHO_GRAFICO = 515;
    private static final float ALTO_GRAFICO = 205;

    private final AdminService adminService;
    private final ReporteIntegralCitasRepository repository;
    private final GeneradorReportesSql generador;

    public ReporteIntegralCitasService(AdminService adminService, ReporteIntegralCitasRepository repository,
                                       GeneradorReportesSql generador) {
        this.adminService = adminService;
        this.repository = repository;
        this.generador = generador;
    }

    public com.cis.citas_salud.api.dto.AdminDto.ReporteIntegralCitasResponse datos(
            UsuarioSesion sesion, LocalDate desde, LocalDate hasta, Integer idEspecialidad,
            Integer idDoctor, String estado) {
        adminService.personal(sesion);
        validarPeriodo(desde, hasta);
        if (idEspecialidad != null && idEspecialidad < 1 || idDoctor != null && idDoctor < 1) {
            throw ApiException.datosInvalidos("El filtro de especialidad o doctor no es válido.");
        }
        Set<String> estadosValidos = Set.of("RESERVADA", "CONFIRMADA", "ATENDIDA", "NO_ASISTIO", "CANCELADA");
        if (estado != null && !estadosValidos.contains(estado)) {
            throw ApiException.datosInvalidos("El estado seleccionado no es válido.");
        }
        var estados = repository.integralConteoPorEstado(desde, hasta, idEspecialidad, idDoctor, estado);
        var evolucionDiaria = repository.integralEvolucionDiaria(desde, hasta, idEspecialidad, idDoctor, estado);
        var especialidades = repository.integralPorEspecialidad(desde, hasta, idEspecialidad, idDoctor, estado);
        var detalle = repository.integralDetalle(desde, hasta, idEspecialidad, idDoctor, estado);
        long total = 0, activas = 0, atendidas = 0, noAsistio = 0, canceladas = 0;
        List<com.cis.citas_salud.api.dto.AdminDto.FilaEstadoCitas> porEstado = new java.util.ArrayList<>();
        for (Object[] fila : estados) {
            String clave = String.valueOf(fila[0]); long cantidad = ((Number) fila[1]).longValue();
            total += cantidad;
            if (List.of("RESERVADA", "CONFIRMADA").contains(clave)) activas += cantidad;
            if ("ATENDIDA".equals(clave)) atendidas += cantidad;
            if ("NO_ASISTIO".equals(clave)) noAsistio += cantidad;
            if ("CANCELADA".equals(clave)) canceladas += cantidad;
            porEstado.add(new com.cis.citas_salud.api.dto.AdminDto.FilaEstadoCitas(clave, cantidad));
        }
        boolean mensual = ChronoUnit.DAYS.between(desde, hasta) > 92;
        java.util.Map<String, long[]> grupos = new java.util.TreeMap<>();
        for (Object[] fila : evolucionDiaria) {
            LocalDate fecha = (LocalDate) fila[0];
            LocalDate periodo = mensual ? fecha.withDayOfMonth(1) : fecha.with(java.time.DayOfWeek.MONDAY);
            String clave = periodo.toString();
            long[] valores = grupos.computeIfAbsent(clave, k -> new long[5]);
            String e = String.valueOf(fila[1]); long cantidad = ((Number) fila[2]).longValue();
            valores[0] += cantidad;
            if (List.of("RESERVADA", "CONFIRMADA").contains(e)) valores[1] += cantidad;
            if ("ATENDIDA".equals(e)) valores[2] += cantidad;
            if ("NO_ASISTIO".equals(e)) valores[3] += cantidad;
            if ("CANCELADA".equals(e)) valores[4] += cantidad;
        }
        List<com.cis.citas_salud.api.dto.AdminDto.PuntoEvolucionCitas> evolucion = grupos.entrySet().stream()
                .map(e -> new com.cis.citas_salud.api.dto.AdminDto.PuntoEvolucionCitas(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3], e.getValue()[4])).toList();
        List<com.cis.citas_salud.api.dto.AdminDto.FilaEspecialidadIntegral> porEspecialidad = especialidades.stream()
                .map(f -> new com.cis.citas_salud.api.dto.AdminDto.FilaEspecialidadIntegral(String.valueOf(f[0]), String.valueOf(f[1]), ((Number) f[2]).longValue())).toList();
        List<com.cis.citas_salud.api.dto.AdminDto.FilaDetalleCita> filasDetalle = detalle.stream().limit(1000)
                .map(f -> new com.cis.citas_salud.api.dto.AdminDto.FilaDetalleCita(((Number) f[0]).intValue(), String.valueOf(f[1]), String.valueOf(f[2]), String.valueOf(f[3]), String.valueOf(f[4]) + " " + String.valueOf(f[5]), String.valueOf(f[6]))).toList();
        int tasa = atendidas + noAsistio == 0 ? 0 : (int) Math.round(100.0 * atendidas / (atendidas + noAsistio));
        return new com.cis.citas_salud.api.dto.AdminDto.ReporteIntegralCitasResponse(desde.toString(), hasta.toString(), mensual ? "MENSUAL" : "SEMANAL", total, activas, atendidas, noAsistio, canceladas, tasa, evolucion, porEstado, porEspecialidad, filasDetalle);
    }

    public Archivo pdf(UsuarioSesion sesion, LocalDate desde, LocalDate hasta, List<String> seleccionadas) {
        return pdf(sesion, desde, hasta, seleccionadas, null, null, null);
    }

    public Archivo pdf(UsuarioSesion sesion, LocalDate desde, LocalDate hasta, List<String> seleccionadas,
                       Integer idEspecialidad, Integer idDoctor, String estado) {
        validarSecciones(seleccionadas);
        var reporte = datos(sesion, desde, hasta, idEspecialidad, idDoctor, estado);
        java.util.Map<String, Object> parametros = new java.util.HashMap<>();
        parametros.put("P_DESDE", reporte.desde());
        parametros.put("P_HASTA", reporte.hasta());
        parametros.put("P_TOTAL", reporte.totalProgramadas());
        parametros.put("P_ACTIVAS", reporte.activas());
        parametros.put("P_ATENDIDAS", reporte.atendidas());
        parametros.put("P_NO_ASISTIO", reporte.noAsistio());
        parametros.put("P_CANCELADAS", reporte.canceladas());
        parametros.put("P_TASA_ASISTENCIA", reporte.tasaAsistencia());
        parametros.put("P_GRANULARIDAD", reporte.granularidad());
        parametros.put("P_GRAFICO_EVOLUCION", ReporteIntegralCitasGraficos.evolucion(reporte.evolucion()));
        parametros.put("P_GRAFICO_ESTADOS", ReporteIntegralCitasGraficos.estados(reporte.porEstado()));
        List<java.util.Map<String, ?>> filas = reporte.detalle().stream().<java.util.Map<String, ?>>map(f -> {
            java.util.Map<String, Object> fila = new java.util.HashMap<>();
            fila.put("fecha", f.fecha()); fila.put("hora", f.hora()); fila.put("especialidad", f.especialidad());
            fila.put("doctor", f.doctor()); fila.put("estado", f.estado()); return fila;
        }).toList();
        byte[] contenido = generador.pdf("ReporteIntegralCitas", parametros,
                new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(filas));
        return new Archivo("reporte-integral-citas-" + desde + "-a-" + hasta + ".pdf", "application/pdf", contenido);
    }

    static Set<String> validarSecciones(List<String> seleccionadas) {
        if (seleccionadas == null) return SECCIONES;
        Set<String> secciones = Set.copyOf(seleccionadas);
        if (secciones.isEmpty()) throw ApiException.datosInvalidos("Selecciona al menos una consulta para el reporte.");
        if (!SECCIONES.containsAll(secciones)) throw ApiException.datosInvalidos("Una de las consultas seleccionadas no es válida.");
        return secciones;
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

    static byte[] renderizar(LocalDate desde, LocalDate hasta, List<Object[]> estados,
                             List<Object[]> especialidades, List<Object[]> doctores,
                             List<Object[]> cambios, List<Object[]> derivaciones, Set<String> secciones) {
        Document documento = new Document(PageSize.A4, 38, 38, 38, 38);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("REPORTE INTEGRAL DE CITAS", TITULO));
            documento.add(new Paragraph("Periodo: " + desde + " al " + hasta, TEXTO));
            if (secciones.contains("RESUMEN")) {
                documento.add(new Paragraph("1. Comparación de resultados y eventos", SUBTITULO));
                List<Object[]> comparacion = comparacion(estados, cambios, derivaciones);
                tabla(documento, new String[]{"Resultado o evento", "Cantidad"}, comparacion);
                graficoColumnas(escritor, documento, comparacion);
                documento.add(new Paragraph("Atenciones, cancelaciones, reprogramaciones y derivaciones pueden superponerse; no son partes excluyentes del total.", TEXTO));
                documento.add(new Paragraph("Distribución exclusiva de citas por estado", SUBTITULO));
                List<Object[]> estadosLegibles = traducirEstados(estados);
                tabla(documento, new String[]{"Estado de cita", "Citas"}, estadosLegibles);
                graficoTorta(escritor, documento, estadosLegibles);
            }
            if (secciones.contains("ESPECIALIDAD")) {
                documento.add(new Paragraph("2. Citas por especialidad y estado", SUBTITULO));
                tabla(documento, new String[]{"Especialidad", "Estado", "Citas"}, especialidades);
                graficoColumnas(escritor, documento, totalesPorEspecialidad(especialidades));
            }
            if (secciones.contains("DOCTORES")) {
                documento.add(new Paragraph("3. Cinco doctores con más citas solicitadas", SUBTITULO));
                List<Object[]> doctoresReporte = combinarDoctor(doctores);
                tabla(documento, new String[]{"Doctor", "Especialidad", "Citas"}, doctoresReporte);
                graficoColumnas(escritor, documento, doctoresReporte.stream().map(f -> new Object[]{f[0], f[2]}).toList());
            }
            if (secciones.contains("CAMBIOS")) {
                documento.add(new Paragraph("4. Cambios de fecha resueltos", SUBTITULO));
                List<Object[]> decisiones = traducirDecisiones(cambios);
                tabla(documento, new String[]{"Decisión del paciente", "Solicitudes"}, decisiones);
                graficoTorta(escritor, documento, decisiones);
                documento.add(new Paragraph("Solo se consideran cambios completados con decisión MISMO_DOCTOR u OTRO_DOCTOR.", TEXTO));
            }
            if (secciones.contains("DERIVACIONES")) {
                documento.add(new Paragraph("5. Derivaciones hospitalarias", SUBTITULO));
                List<Object[]> estadosSolicitud = traducirEstadosSolicitud(derivaciones);
                tabla(documento, new String[]{"Estado de solicitud", "Solicitudes"}, estadosSolicitud);
                graficoTorta(escritor, documento, estadosSolicitud);
            }
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte integral de citas.", e);
        } finally {
            if (documento.isOpen()) documento.close();
        }
    }

    private static List<Object[]> comparacion(List<Object[]> estados, List<Object[]> cambios,
                                              List<Object[]> derivaciones) {
        long atendidas = sumarEstado(estados, "ATENDIDA");
        long canceladas = sumarEstado(estados, "CANCELADA");
        long postergadas = cambios.stream()
                .filter(f -> "MISMO_DOCTOR".equals(String.valueOf(f[0])) || "OTRO_DOCTOR".equals(String.valueOf(f[0])))
                .mapToLong(f -> ((Number) f[1]).longValue()).sum();
        long derivadas = derivaciones.stream().mapToLong(f -> ((Number) f[1]).longValue()).sum();
        return List.of(new Object[]{"Atendidas", atendidas}, new Object[]{"Canceladas", canceladas},
                new Object[]{"Postergadas completadas", postergadas}, new Object[]{"Derivaciones solicitadas", derivadas});
    }

    private static long sumarEstado(List<Object[]> filas, String estado) {
        return filas.stream().filter(f -> estado.equals(String.valueOf(f[0])))
                .mapToLong(f -> ((Number) f[1]).longValue()).sum();
    }

    /** Gráfico de columnas vertical, con eje numérico, cuadrícula y una serie multicolor. */
    private static void graficoColumnas(PdfWriter escritor, Document documento, List<Object[]> filas)
            throws DocumentException, IOException {
        if (filas.isEmpty()) return;
        PdfTemplate grafico = escritor.getDirectContent().createTemplate(ANCHO_GRAFICO, ALTO_GRAFICO);
        PdfContentByte lienzo = grafico;
        BaseFont fuente = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        float izquierda = 46;
        float abajo = 38;
        float alto = 128;
        float ancho = ANCHO_GRAFICO - izquierda - 10;
        long maximo = filas.stream().mapToLong(f -> ((Number) f[f.length - 1]).longValue()).max().orElse(0);
        if (maximo == 0) maximo = 1;

        lienzo.saveState();
        lienzo.setColorStroke(new Color(203, 213, 225));
        lienzo.setLineWidth(0.6f);
        for (int nivel = 0; nivel <= 4; nivel++) {
            float y = abajo + alto * nivel / 4;
            lienzo.moveTo(izquierda, y);
            lienzo.lineTo(izquierda + ancho, y);
            lienzo.stroke();
            textoGrafico(lienzo, fuente, Long.toString(Math.round(maximo * nivel / 4.0)), izquierda - 7, y - 2, 7, PdfContentByte.ALIGN_RIGHT);
        }
        lienzo.setColorStroke(new Color(71, 85, 105));
        lienzo.moveTo(izquierda, abajo);
        lienzo.lineTo(izquierda, abajo + alto);
        lienzo.lineTo(izquierda + ancho, abajo);
        lienzo.stroke();

        float espacio = ancho / filas.size();
        float barraAncho = Math.min(38, espacio * 0.58f);
        for (int i = 0; i < filas.size(); i++) {
            Object[] fila = filas.get(i);
            String etiqueta = abreviar(String.valueOf(fila[0]), 13);
            long cantidad = ((Number) fila[fila.length - 1]).longValue();
            float altoBarra = alto * cantidad / maximo;
            float x = izquierda + espacio * (i + 0.5f);
            lienzo.setColorFill(COLORES_GRAFICO[i % COLORES_GRAFICO.length]);
            lienzo.rectangle(x - barraAncho / 2, abajo, barraAncho, altoBarra);
            lienzo.fill();
            textoGrafico(lienzo, fuente, Long.toString(cantidad), x, abajo + altoBarra + 5, 8, PdfContentByte.ALIGN_CENTER);
            textoGrafico(lienzo, fuente, etiqueta, x, abajo - 13, 7, PdfContentByte.ALIGN_CENTER);
        }
        lienzo.restoreState();
        Image imagen = Image.getInstance(grafico);
        imagen.setAlignment(Image.ALIGN_CENTER);
        imagen.setSpacingBefore(5);
        imagen.setSpacingAfter(10);
        documento.add(imagen);
    }

    /** Gráfico de torta para distribuciones cuyas categorías no se solapan. */
    private static void graficoTorta(PdfWriter escritor, Document documento, List<Object[]> filas)
            throws DocumentException, IOException {
        long total = filas.stream().mapToLong(f -> ((Number) f[f.length - 1]).longValue()).sum();
        if (filas.isEmpty() || total == 0) return;
        PdfTemplate grafico = escritor.getDirectContent().createTemplate(ANCHO_GRAFICO, ALTO_GRAFICO);
        PdfContentByte lienzo = grafico;
        BaseFont fuente = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        float centroX = 100;
        float centroY = 102;
        float radio = 72;
        double angulo = 90;
        for (int i = 0; i < filas.size(); i++) {
            long cantidad = ((Number) filas.get(i)[filas.get(i).length - 1]).longValue();
            double barrido = 360.0 * cantidad / total;
            dibujarSector(lienzo, centroX, centroY, radio, angulo, barrido, COLORES_GRAFICO[i % COLORES_GRAFICO.length]);
            angulo += barrido;
        }
        for (int i = 0; i < filas.size(); i++) {
            Object[] fila = filas.get(i);
            long cantidad = ((Number) fila[fila.length - 1]).longValue();
            float y = 158 - i * 20;
            lienzo.setColorFill(COLORES_GRAFICO[i % COLORES_GRAFICO.length]);
            lienzo.rectangle(205, y - 2, 11, 11);
            lienzo.fill();
            String etiqueta = String.valueOf(fila[0]) + " — " + cantidad + " (" + Math.round(100.0 * cantidad / total) + " %)";
            textoGrafico(lienzo, fuente, abreviar(etiqueta, 50), 224, y, 9, PdfContentByte.ALIGN_LEFT);
        }
        Image imagen = Image.getInstance(grafico);
        imagen.setAlignment(Image.ALIGN_CENTER);
        imagen.setSpacingBefore(5);
        imagen.setSpacingAfter(10);
        documento.add(imagen);
    }

    private static void dibujarSector(PdfContentByte lienzo, float centroX, float centroY, float radio,
                                     double inicio, double barrido, Color color) {
        lienzo.saveState();
        lienzo.setColorFill(color);
        lienzo.setColorStroke(Color.WHITE);
        lienzo.setLineWidth(1.2f);
        lienzo.moveTo(centroX, centroY);
        double angulo = Math.toRadians(inicio);
        float x = (float) (centroX + radio * Math.cos(angulo));
        float y = (float) (centroY + radio * Math.sin(angulo));
        lienzo.lineTo(x, y);
        int segmentos = Math.max(1, (int) Math.ceil(Math.abs(barrido) / 90.0));
        double paso = Math.toRadians(barrido / segmentos);
        for (int i = 0; i < segmentos; i++) {
            double fin = angulo + paso;
            double k = 4.0 / 3.0 * Math.tan(paso / 4.0);
            float xFin = (float) (centroX + radio * Math.cos(fin));
            float yFin = (float) (centroY + radio * Math.sin(fin));
            float xControl1 = (float) (x - k * radio * Math.sin(angulo));
            float yControl1 = (float) (y + k * radio * Math.cos(angulo));
            float xControl2 = (float) (xFin + k * radio * Math.sin(fin));
            float yControl2 = (float) (yFin - k * radio * Math.cos(fin));
            lienzo.curveTo(xControl1, yControl1, xControl2, yControl2, xFin, yFin);
            angulo = fin;
            x = xFin;
            y = yFin;
        }
        lienzo.closePath();
        lienzo.fillStroke();
        lienzo.restoreState();
    }

    private static void textoGrafico(PdfContentByte lienzo, BaseFont fuente, String texto,
                                     float x, float y, float tamano, int alineacion) {
        lienzo.beginText();
        lienzo.setFontAndSize(fuente, tamano);
        lienzo.setColorFill(new Color(51, 65, 85));
        lienzo.showTextAligned(alineacion, texto, x, y, 0);
        lienzo.endText();
    }

    private static String abreviar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private static com.lowagie.text.pdf.PdfPCell celda(String texto) {
        return new com.lowagie.text.pdf.PdfPCell(new Phrase(texto, TEXTO));
    }

    private static List<Object[]> totalesPorEspecialidad(List<Object[]> filas) {
        java.util.Map<String, Long> totales = new java.util.TreeMap<>();
        for (Object[] fila : filas) totales.merge(String.valueOf(fila[0]), ((Number) fila[2]).longValue(), Long::sum);
        return totales.entrySet().stream().map(e -> new Object[]{e.getKey(), e.getValue()}).toList();
    }

    private static List<Object[]> traducirEstados(List<Object[]> filas) {
        return filas.stream().map(f -> new Object[]{traducirEstado(String.valueOf(f[0])), f[1]}).toList();
    }

    private static List<Object[]> traducirEstadosSolicitud(List<Object[]> filas) {
        return filas.stream().map(f -> new Object[]{traducirEstadoSolicitud(String.valueOf(f[0])), f[1]}).toList();
    }

    private static List<Object[]> traducirDecisiones(List<Object[]> filas) {
        return filas.stream().map(f -> new Object[]{switch (String.valueOf(f[0])) {
            case "MISMO_DOCTOR" -> "Con el mismo doctor";
            case "OTRO_DOCTOR" -> "Con otro doctor";
            default -> String.valueOf(f[0]);
        }, f[1]}).toList();
    }

    private static String traducirEstado(String estado) {
        return switch (estado) {
            case "ATENDIDA" -> "Atendida";
            case "CANCELADA" -> "Cancelada";
            case "NO_ASISTIO" -> "No asistió";
            case "RESERVADA" -> "Reservada";
            case "CONFIRMADA" -> "Confirmada";
            default -> estado;
        };
    }

    private static String traducirEstadoSolicitud(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> "Pendiente";
            case "APROBADA" -> "Aprobada";
            case "RECHAZADA" -> "Rechazada";
            case "RESUELTA" -> "Resuelta";
            default -> estado;
        };
    }

    private static List<Object[]> combinarDoctor(List<Object[]> filas) {
        return filas.stream().map(f -> new Object[]{f[0] + " " + f[1], f[2], f[3]}).toList();
    }

    private static void tabla(Document documento, String[] titulos, List<Object[]> filas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(titulos.length);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);
        tabla.setSpacingAfter(7);
        for (String titulo : titulos) {
            com.lowagie.text.pdf.PdfPCell celda = new com.lowagie.text.pdf.PdfPCell(new Phrase(titulo, CABECERA));
            celda.setBackgroundColor(new Color(37, 99, 116));
            celda.setPadding(5);
            tabla.addCell(celda);
        }
        for (Object[] fila : filas) {
            for (int indice = 0; indice < fila.length; indice++) {
                Object valor = fila[indice];
                if (indice == 1 && fila.length == 3 && fila[2] instanceof Number) valor = traducirEstado(String.valueOf(valor));
                com.lowagie.text.pdf.PdfPCell celda = new com.lowagie.text.pdf.PdfPCell(new Phrase(String.valueOf(valor), TEXTO));
                celda.setPadding(4);
                tabla.addCell(celda);
            }
        }
        if (filas.isEmpty()) {
            com.lowagie.text.pdf.PdfPCell vacia = new com.lowagie.text.pdf.PdfPCell(new Phrase("Sin registros en el periodo", TEXTO));
            vacia.setColspan(titulos.length);
            vacia.setPadding(5);
            tabla.addCell(vacia);
        }
        documento.add(tabla);
    }
}
