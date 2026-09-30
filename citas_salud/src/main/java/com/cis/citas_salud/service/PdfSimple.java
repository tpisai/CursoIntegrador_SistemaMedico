package com.cis.citas_salud.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un PDF de una página con texto plano (ticket de atención y documentos de
 * demostración). Usa las fuentes estándar del PDF, así que no necesita librerías.
 */
public final class PdfSimple {

    private PdfSimple() {
    }

    public static byte[] crear(String titulo, List<String> lineas) {
        StringBuilder contenido = new StringBuilder()
                .append("BT\n/F2 18 Tf 56 780 Td\n(").append(escapar(titulo)).append(") Tj\n")
                .append("/F1 11 Tf 0 -30 Td 16 TL\n");
        for (String linea : lineas) {
            contenido.append('(').append(escapar(linea)).append(") Tj T*\n");
        }
        contenido.append("ET");

        List<String> objetos = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>",
                "<< /Length " + contenido.length() + " >>\nstream\n" + contenido + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        // En ISO-8859-1 cada carácter ocupa 1 byte, así las posiciones del índice (xref) cuadran.
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
        }
        int inicioXref = pdf.length();
        pdf.append("xref\n0 ").append(objetos.size() + 1).append("\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            pdf.append(String.format("%010d 00000 n \n", posicion));
        }
        pdf.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(inicioXref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String escapar(String texto) {
        StringBuilder limpio = new StringBuilder();
        for (char c : texto.toCharArray()) {
            limpio.append(c < 256 ? c : '?'); // Fuera de Latin-1 (p. ej. "—") no se puede dibujar.
        }
        return limpio.toString().replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
