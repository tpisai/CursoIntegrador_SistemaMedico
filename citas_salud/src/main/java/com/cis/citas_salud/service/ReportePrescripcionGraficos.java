package com.cis.citas_salud.service;

import com.cis.citas_salud.api.dto.ReportePrescripcionDto.DiagnosticoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.MedicamentoFrecuente;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.TendenciaSemanal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class ReportePrescripcionGraficos {

    private static final Color TINTA = new Color(31, 55, 70);
    private static final Color SECUNDARIO = new Color(100, 116, 139);
    private static final Color CUADRICULA = new Color(226, 232, 240);
    private static final Color TURQUESA = new Color(13, 148, 136);
    private static final int ANCHO = 1100;
    private static final int ALTO_BARRAS = 440;
    private static final int ALTO_LINEA = 380;

    private ReportePrescripcionGraficos() {
    }

    static BufferedImage medicamentos(List<MedicamentoFrecuente> filas) {
        return barras("Medicamentos más recetados", filas.stream()
                .map(fila -> new Dato(fila.nombre(), fila.cantidad())).toList());
    }

    static BufferedImage diagnosticos(List<DiagnosticoFrecuente> filas) {
        return barras("Diagnósticos frecuentes", filas.stream()
                .map(fila -> new Dato(fila.nombre(), fila.atenciones())).toList());
    }

    static BufferedImage tendencia(List<TendenciaSemanal> filas) {
        BufferedImage imagen = lienzo(ALTO_LINEA);
        Graphics2D grafico = imagen.createGraphics();
        preparar(grafico);
        titulo(grafico, "Tendencia semanal de recetas");
        int izquierda = 82;
        int derecha = ANCHO - 36;
        int arriba = 76;
        int abajo = ALTO_LINEA - 64;
        long maximo = Math.max(1, filas.stream().mapToLong(TendenciaSemanal::recetas).max().orElse(0));
        dibujarEje(grafico, izquierda, derecha, arriba, abajo, maximo);

        if (filas.isEmpty()) {
            mensajeVacio(grafico, arriba, abajo);
        } else {
            grafico.setColor(TURQUESA);
            grafico.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int puntos = filas.size();
            int pasoEtiqueta = Math.max(1, (int) Math.ceil(puntos / 8.0));
            int xAnterior = 0;
            int yAnterior = 0;
            for (int indice = 0; indice < puntos; indice++) {
                TendenciaSemanal fila = filas.get(indice);
                int x = puntos == 1 ? (izquierda + derecha) / 2
                        : izquierda + (derecha - izquierda) * indice / (puntos - 1);
                int y = abajo - (int) Math.round((abajo - arriba) * (double) fila.recetas() / maximo);
                if (indice > 0) grafico.drawLine(xAnterior, yAnterior, x, y);
                grafico.fillOval(x - 6, y - 6, 12, 12);
                if (indice % pasoEtiqueta == 0 || indice == puntos - 1) {
                    grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
                    grafico.setColor(SECUNDARIO);
                    grafico.drawString(formatoSemana(fila.semana()), x - 24, abajo + 27);
                    grafico.setColor(TURQUESA);
                }
                xAnterior = x;
                yAnterior = y;
            }
        }
        grafico.dispose();
        return imagen;
    }

    private static BufferedImage barras(String titulo, List<Dato> filas) {
        BufferedImage imagen = lienzo(ALTO_BARRAS);
        Graphics2D grafico = imagen.createGraphics();
        preparar(grafico);
        titulo(grafico, titulo);
        if (filas.isEmpty()) {
            mensajeVacio(grafico, 75, ALTO_BARRAS - 35);
            grafico.dispose();
            return imagen;
        }

        int margenIzquierdo = 330;
        int margenDerecho = ANCHO - 55;
        int altoUtil = ALTO_BARRAS - 118;
        int altoFila = Math.max(27, altoUtil / filas.size());
        int altoBarra = Math.min(20, altoFila - 7);
        long maximo = Math.max(1, filas.stream().mapToLong(Dato::cantidad).max().orElse(0));
        dibujarEje(grafico, margenIzquierdo, margenDerecho, 76, 76 + altoFila * filas.size(), maximo);
        grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        FontMetrics metricas = grafico.getFontMetrics();
        for (int indice = 0; indice < filas.size(); indice++) {
            Dato fila = filas.get(indice);
            int y = 93 + indice * altoFila;
            String nombre = abreviar(fila.nombre(), 38);
            grafico.setColor(TINTA);
            grafico.drawString(nombre, margenIzquierdo - 16 - metricas.stringWidth(nombre), y + altoBarra - 2);
            int ancho = (int) Math.round((margenDerecho - margenIzquierdo) * (double) fila.cantidad() / maximo);
            grafico.setColor(TURQUESA);
            grafico.fillRoundRect(margenIzquierdo, y, Math.max(fila.cantidad() > 0 ? 2 : 0, ancho), altoBarra, 8, 8);
            grafico.setColor(TINTA);
            grafico.drawString(Long.toString(fila.cantidad()), margenIzquierdo + ancho + 9, y + altoBarra - 2);
        }
        grafico.dispose();
        return imagen;
    }

    private static void dibujarEje(Graphics2D grafico, int izquierda, int derecha, int arriba, int abajo, long maximo) {
        grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        for (int marca = 0; marca <= 4; marca++) {
            int x = izquierda + (derecha - izquierda) * marca / 4;
            grafico.setColor(CUADRICULA);
            grafico.drawLine(x, arriba, x, abajo);
            grafico.setColor(SECUNDARIO);
            String etiqueta = Long.toString(Math.round(maximo * marca / 4.0));
            grafico.drawString(etiqueta, x - grafico.getFontMetrics().stringWidth(etiqueta) / 2, abajo + 18);
        }
    }

    private static BufferedImage lienzo(int alto) {
        BufferedImage imagen = new BufferedImage(ANCHO, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = imagen.createGraphics();
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, ANCHO, alto);
        grafico.dispose();
        return imagen;
    }

    private static void preparar(Graphics2D grafico) {
        grafico.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        grafico.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void titulo(Graphics2D grafico, String texto) {
        grafico.setColor(TINTA);
        grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        grafico.drawString(texto, 34, 42);
    }

    private static void mensajeVacio(Graphics2D grafico, int arriba, int abajo) {
        String mensaje = "Sin datos para mostrar en este periodo.";
        grafico.setColor(SECUNDARIO);
        grafico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        grafico.drawString(mensaje, 35, arriba + (abajo - arriba) / 2);
    }

    private static String formatoSemana(String fecha) {
        return LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    private static String abreviar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private record Dato(String nombre, long cantidad) {
    }
}
