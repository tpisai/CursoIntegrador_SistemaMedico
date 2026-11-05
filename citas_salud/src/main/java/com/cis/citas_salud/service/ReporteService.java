package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.FilaReporte;
import com.cis.citas_salud.api.dto.AdminDto.ReporteResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reporte de control de administración (EDT 4.8): citas y ausentismo por especialidad y por doctor. */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final int MAX_DIAS = 366;

    private final AdminService adminService;
    private final CitaRepository citaRepository;

    public ReporteService(AdminService adminService, CitaRepository citaRepository) {
        this.adminService = adminService;
        this.citaRepository = citaRepository;
    }

    /** Sin fechas, muestra el mes en curso hasta hoy. */
    public ReporteResponse citas(UsuarioSesion sesion, LocalDate desde, LocalDate hasta) {
        adminService.personal(sesion);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        if (fin.isBefore(inicio)) {
            throw ApiException.datosInvalidos("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > MAX_DIAS) {
            throw ApiException.datosInvalidos("El reporte abarca como máximo un año.");
        }

        Conteo totales = new Conteo("Total", null);
        Map<String, Conteo> especialidades = new LinkedHashMap<>();
        Map<String, Conteo> doctores = new LinkedHashMap<>();
        for (Object[] fila : citaRepository.conteoParaReporte(inicio, fin)) {
            String especialidad = (String) fila[0];
            String doctor = fila[1] + " " + fila[2];
            String estado = (String) fila[3];
            long citas = (Long) fila[4];
            totales.sumar(estado, citas);
            especialidades.computeIfAbsent(especialidad, e -> new Conteo(e, null)).sumar(estado, citas);
            doctores.computeIfAbsent(doctor, d -> new Conteo(d, especialidad)).sumar(estado, citas);
        }
        return new ReporteResponse(Formato.fecha(inicio), Formato.fecha(fin), totales.fila(),
                ordenadas(especialidades), ordenadas(doctores));
    }

    private static List<FilaReporte> ordenadas(Map<String, Conteo> conteos) {
        return conteos.values().stream()
                .map(Conteo::fila)
                .sorted(Comparator.comparingLong(FilaReporte::total).reversed().thenComparing(FilaReporte::nombre))
                .toList();
    }

    private static final class Conteo {

        private final String nombre;
        private final String detalle;
        private long total;
        private long atendidas;
        private long noAsistio;
        private long canceladas;
        private long pendientes;

        private Conteo(String nombre, String detalle) {
            this.nombre = nombre;
            this.detalle = detalle;
        }

        private void sumar(String estado, long citas) {
            total += citas;
            switch (estado) {
                case Cita.ATENDIDA -> atendidas += citas;
                case Cita.NO_ASISTIO -> noAsistio += citas;
                case Cita.CANCELADA -> canceladas += citas;
                default -> pendientes += citas;
            }
        }

        private FilaReporte fila() {
            long cerradas = atendidas + noAsistio;
            int ausentismo = cerradas == 0 ? 0 : (int) Math.round(100.0 * noAsistio / cerradas);
            return new FilaReporte(nombre, detalle, total, atendidas, noAsistio, canceladas, pendientes, ausentismo);
        }
    }
}
