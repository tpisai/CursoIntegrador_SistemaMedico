package com.cis.citas_salud.service;

import com.cis.citas_salud.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Formatos que espera el frontend: fechas "YYYY-MM-DD", horas "HH:mm". */
public final class Formato {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};

    private Formato() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha.toString();
    }

    public static String hora(LocalTime hora) {
        return hora.format(HORA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /** "Mar 22 sep · 09:30", igual que en el frontend. */
    public static String diaHora(LocalDate fecha, LocalTime hora) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.getDayOfMonth() + " "
                + MESES[fecha.getMonthValue() - 1] + " · " + hora(hora);
    }

    public static String fechaLegible(LocalDate fecha) {
        return fecha.format(FECHA_LEGIBLE);
    }

    /** "Luis Ramírez H.": primer nombre, primer apellido e inicial del segundo. */
    public static String nombrePaciente(Usuario usuario) {
        String[] apellidos = usuario.getApellidos().trim().split("\\s+");
        String inicial = apellidos.length > 1 ? " " + apellidos[1].charAt(0) + "." : "";
        return usuario.getNombres().trim().split("\\s+")[0] + " " + apellidos[0] + inicial;
    }

    public static String ticket(long numero) {
        return "A-" + String.format("%04d", numero);
    }
}
