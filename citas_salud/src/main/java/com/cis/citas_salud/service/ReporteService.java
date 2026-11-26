package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.FilaOcupacion;
import com.cis.citas_salud.api.dto.AdminDto.FilaReporte;
import com.cis.citas_salud.api.dto.AdminDto.FiltrosReporteResponse;
import com.cis.citas_salud.api.dto.AdminDto.OpcionReporte;
import com.cis.citas_salud.api.dto.AdminDto.ReporteOcupacionResponse;
import com.cis.citas_salud.api.dto.AdminDto.ReporteResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reportes de control de administración: citas/ausentismo y ocupación de capacidad horaria. */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final int MAX_DIAS = 366;
    private static final String ACTIVO = "ACTIVO";
    private static final Set<String> ESTADOS_DE_CITA =
            Set.of(Cita.RESERVADA, Cita.CONFIRMADA, Cita.ATENDIDA, Cita.NO_ASISTIO, Cita.CANCELADA);

    private final AdminService adminService;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;

    public ReporteService(AdminService adminService, CitaRepository citaRepository, HorarioRepository horarioRepository,
                          DoctorRepository doctorRepository, EspecialidadRepository especialidadRepository) {
        this.adminService = adminService;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
    }

    /** Opciones de los filtros del reporte de citas: especialidades y doctores activos. */
    public FiltrosReporteResponse filtros(UsuarioSesion sesion) {
        adminService.personal(sesion);
        List<OpcionReporte> especialidades = especialidadRepository.findByEstadoOrderByIdEspecialidad(ACTIVO).stream()
                .map(e -> new OpcionReporte(e.getIdEspecialidad(), e.getNombre(), null))
                .sorted(Comparator.comparing(OpcionReporte::nombre))
                .toList();
        // El detalle del doctor es su especialidad: el formulario lo usa para acotar la lista.
        List<OpcionReporte> doctores = doctorRepository.findByEstadoOrderByIdDoctor(ACTIVO).stream()
                .map(d -> new OpcionReporte(d.getIdDoctor(),
                        d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos(),
                        d.getEspecialidad().getNombre()))
                .sorted(Comparator.comparing(OpcionReporte::nombre))
                .toList();
        return new FiltrosReporteResponse(especialidades, doctores);
    }

    /** Sin fechas, muestra el mes en curso hasta hoy. */
    public ReporteResponse citas(UsuarioSesion sesion, LocalDate desde, LocalDate hasta) {
        return citas(sesion, desde, hasta, null, null, null);
    }

    /** Reporte de citas acotado a una especialidad, un doctor o un estado de cita; null = sin ese filtro. */
    public ReporteResponse citas(UsuarioSesion sesion, LocalDate desde, LocalDate hasta,
                                 Integer idEspecialidad, Integer idDoctor, String estado) {
        adminService.personal(sesion);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        validarRango(inicio, fin);
        String estadoCita = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        if (estadoCita != null && !ESTADOS_DE_CITA.contains(estadoCita)) {
            throw ApiException.datosInvalidos("El estado de cita no es válido.");
        }

        Conteo totales = new Conteo(null, null, "Total", null);
        // Se agrupa por id: dos doctores con el mismo nombre no se mezclan.
        Map<Integer, Conteo> especialidades = new LinkedHashMap<>();
        Map<Integer, Conteo> doctores = new LinkedHashMap<>();
        for (Object[] fila : citaRepository.conteoParaReporte(inicio, fin, idEspecialidad, idDoctor, estadoCita)) {
            Integer especialidadId = (Integer) fila[0];
            String especialidad = (String) fila[1];
            Integer doctorId = (Integer) fila[2];
            String doctor = fila[3] + " " + fila[4];
            String estadoFila = (String) fila[5];
            long citas = (Long) fila[6];
            totales.sumar(estadoFila, citas);
            especialidades.computeIfAbsent(especialidadId, id -> new Conteo(id, null, especialidad, null))
                    .sumar(estadoFila, citas);
            doctores.computeIfAbsent(doctorId, id -> new Conteo(especialidadId, id, doctor, especialidad))
                    .sumar(estadoFila, citas);
        }
        return new ReporteResponse(Formato.fecha(inicio), Formato.fecha(fin), totales.fila(),
                ordenadas(especialidades), ordenadas(doctores));
    }

    /** Consulta administrativa: aprovechamiento y ocupación de turnos ofertados en agenda. */
    public ReporteOcupacionResponse ocupacionHorarios(UsuarioSesion sesion, LocalDate desde, LocalDate hasta) {
        adminService.personal(sesion);
        LocalDate fin = hasta != null ? hasta : LocalDate.now().plusDays(15);
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        validarRango(inicio, fin);

        ConteoOcupacion totales = new ConteoOcupacion("Total", null);
        Map<String, ConteoOcupacion> especialidades = new LinkedHashMap<>();
        Map<String, ConteoOcupacion> doctores = new LinkedHashMap<>();

        for (Object[] fila : horarioRepository.conteoOcupacion(inicio, fin)) {
            String especialidad = (String) fila[0];
            String doctor = fila[1] + " " + fila[2];
            String estado = (String) fila[3];
            long cantidad = (Long) fila[4];

            totales.sumar(estado, cantidad);
            especialidades.computeIfAbsent(especialidad, e -> new ConteoOcupacion(e, null)).sumar(estado, cantidad);
            doctores.computeIfAbsent(doctor, d -> new ConteoOcupacion(d, especialidad)).sumar(estado, cantidad);
        }

        return new ReporteOcupacionResponse(
                Formato.fecha(inicio),
                Formato.fecha(fin),
                totales.fila(),
                ordenadasOcupacion(especialidades),
                ordenadasOcupacion(doctores));
    }

    private static void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw ApiException.datosInvalidos("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > MAX_DIAS) {
            throw ApiException.datosInvalidos("El reporte abarca como máximo un año.");
        }
    }

    private static List<FilaReporte> ordenadas(Map<Integer, Conteo> conteos) {
        return conteos.values().stream()
                .map(Conteo::fila)
                .sorted(Comparator.comparingLong(FilaReporte::total).reversed().thenComparing(FilaReporte::nombre))
                .toList();
    }

    private static List<FilaOcupacion> ordenadasOcupacion(Map<String, ConteoOcupacion> conteos) {
        return conteos.values().stream()
                .map(ConteoOcupacion::fila)
                .sorted(Comparator.comparingLong(FilaOcupacion::ofertados).reversed().thenComparing(FilaOcupacion::nombre))
                .toList();
    }

    private static final class Conteo {
        // Ids para que el formulario pueda filtrar por la fila; null en los totales.
        private final Integer idEspecialidad;
        private final Integer idDoctor;
        private final String nombre;
        private final String detalle;
        private long total;
        private long atendidas;
        private long noAsistio;
        private long canceladas;
        private long pendientes;

        private Conteo(Integer idEspecialidad, Integer idDoctor, String nombre, String detalle) {
            this.idEspecialidad = idEspecialidad;
            this.idDoctor = idDoctor;
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
            return new FilaReporte(idEspecialidad, idDoctor, nombre, detalle, total, atendidas, noAsistio, canceladas,
                    pendientes, ausentismo);
        }
    }

    private static final class ConteoOcupacion {
        private final String nombre;
        private final String detalle;
        private long ofertados;
        private long ocupados;
        private long disponibles;
        private long cancelados;

        private ConteoOcupacion(String nombre, String detalle) {
            this.nombre = nombre;
            this.detalle = detalle;
        }

        private void sumar(String estado, long cantidad) {
            ofertados += cantidad;
            switch (estado) {
                case Horario.RESERVADO, Horario.FINALIZADO -> ocupados += cantidad;
                case Horario.DISPONIBLE -> disponibles += cantidad;
                case Horario.CANCELADO -> cancelados += cantidad;
                default -> disponibles += cantidad;
            }
        }

        private FilaOcupacion fila() {
            long activos = ofertados - cancelados;
            int porcentaje = activos <= 0 ? 0 : (int) Math.round(100.0 * ocupados / activos);
            return new FilaOcupacion(nombre, detalle, ofertados, ocupados, disponibles, cancelados, porcentaje);
        }
    }
}