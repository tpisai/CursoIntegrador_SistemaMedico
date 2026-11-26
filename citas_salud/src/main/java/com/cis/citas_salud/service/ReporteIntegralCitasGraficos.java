package com.cis.citas_salud.service;

import com.cis.citas_salud.api.dto.AdminDto.PuntoEvolucionCitas;
import com.cis.citas_salud.api.dto.AdminDto.FilaEstadoCitas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

final class ReporteIntegralCitasGraficos {
    private ReporteIntegralCitasGraficos() { }

    static BufferedImage evolucion(List<PuntoEvolucionCitas> filas) {
        BufferedImage imagen = new BufferedImage(900, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, 900, 300);
        g.setFont(new Font("SansSerif", Font.BOLD, 20)); g.setColor(new Color(31,55,70)); g.drawString("Evolución de citas", 24, 30);
        long max = Math.max(1, filas.stream().mapToLong(PuntoEvolucionCitas::programadas).max().orElse(0));
        int ancho = filas.isEmpty() ? 0 : Math.min(60, 820 / filas.size());
        for (int i=0;i<filas.size();i++) { var f=filas.get(i); int h=(int)(210.0*f.programadas()/max); int x=40+i* (820/Math.max(1,filas.size()));
            g.setColor(new Color(13,148,136)); g.fillRoundRect(x,250-h,ancho,h,8,8); g.setColor(new Color(71,85,105));
            g.setFont(new Font("SansSerif",Font.PLAIN,11)); g.drawString(f.periodo(),x,270); g.drawString(Long.toString(f.programadas()),x,240-h);
        }
        g.dispose(); return imagen;
    }

    static BufferedImage estados(List<FilaEstadoCitas> filas) {
        BufferedImage imagen = new BufferedImage(900, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=imagen.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0,0,900,300);
        g.setFont(new Font("SansSerif",Font.BOLD,20)); g.setColor(new Color(31,55,70)); g.drawString("Distribución por estado",24,30);
        long max=Math.max(1,filas.stream().mapToLong(FilaEstadoCitas::cantidad).max().orElse(0));
        for(int i=0;i<filas.size();i++){var f=filas.get(i);int y=65+i*42;int w=(int)(620.0*f.cantidad()/max);g.setColor(new Color(37,99,235));g.fillRoundRect(220,y,Math.max(2,w),24,6,6);g.setColor(new Color(31,55,70));g.setFont(new Font("SansSerif",Font.PLAIN,14));g.drawString(f.estado(),24,y+18);g.drawString(Long.toString(f.cantidad()),850,y+18);}
        g.dispose(); return imagen;
    }
}
