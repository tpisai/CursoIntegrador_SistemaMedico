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
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** EDT 4.8: reporte de control de citas, ausentismo y ocupación de horarios. */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Mock
    private AdminService adminService;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private EspecialidadRepository especialidadRepository;

    private ReporteService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(adminService, citaRepository, horarioRepository, doctorRepository, especialidadRepository);
    }

    private static Object[] fila(String especialidad, String nombres, String apellidos, String estado, long citas) {
        int idEspecialidad = "Pediatría".equals(especialidad) ? 1 : 2;
        int idDoctor = "Patricia".equals(nombres) ? 10 : "Jorge".equals(nombres) ? 11 : 12;
        return new Object[] {idEspecialidad, especialidad, idDoctor, nombres, apellidos, estado, citas};
    }

    /** Fila de HorarioRepository.conteoOcupacion: [especialidad, nombres, apellidos, estado, turnos]. */
    private static Object[] filaOcupacion(String especialidad, String nombres, String apellidos, String estado, long turnos) {
        return new Object[] {especialidad, nombres, apellidos, estado, turnos};
    }

    private void hayCitas(Object[]... filas) {
        when(citaRepository.conteoParaReporte(DESDE, HASTA, null, null, null)).thenReturn(List.of(filas));
    }

    @Test
    void sumaLasCitasPorEstadoEspecialidadYDoctor() {
        hayCitas(fila("Pediatría", "Patricia", "Quiroz Soto", Cita.ATENDIDA, 3),
                fila("Pediatría", "Patricia", "Quiroz Soto", Cita.NO_ASISTIO, 1),
                fila("Pediatría", "Jorge", "Salazar Núñez", Cita.CANCELADA, 2),
                fila("Nutrición", "Josué", "Cóndor Quispe", Cita.RESERVADA, 1),
                fila("Nutrición", "Josué", "Cóndor Quispe", Cita.CONFIRMADA, 1));

        ReporteResponse reporte = servicio.citas(sesion, DESDE, HASTA);

        FilaReporte totales = reporte.totales();
        assertEquals(8, totales.total());
        assertEquals(3, totales.atendidas());
        assertEquals(1, totales.noAsistio());
        assertEquals(2, totales.canceladas());
        assertEquals(2, totales.pendientes());
        assertEquals("Pediatría", reporte.porEspecialidad().get(0).nombre());
        assertEquals(6, reporte.porEspecialidad().get(0).total());
        assertNull(reporte.porEspecialidad().get(0).detalle());
    }

    @Test
    void elAusentismoSoloCuentaLasCitasCerradas() {
        hayCitas(fila("Pediatría", "Patricia", "Quiroz Soto", Cita.ATENDIDA, 3),
                fila("Pediatría", "Patricia", "Quiroz Soto", Cita.NO_ASISTIO, 1),
                fila("Pediatría", "Patricia", "Quiroz Soto", Cita.CANCELADA, 10),
                fila("Pediatría", "Patricia", "Quiroz Soto", Cita.RESERVADA, 10));

        assertEquals(25, servicio.citas(sesion, DESDE, HASTA).totales().ausentismo());
    }

    @Test
    void losDoctoresSeOrdenanPorCitasConSuEspecialidad() {
        hayCitas(fila("Nutrición", "Josué", "Cóndor Quispe", Cita.ATENDIDA, 1),
                fila("Pediatría", "Patricia", "Quiroz Soto", Cita.ATENDIDA, 5));

        List<FilaReporte> doctores = servicio.citas(sesion, DESDE, HASTA).porDoctor();

        assertEquals("Patricia Quiroz Soto", doctores.get(0).nombre());
        assertEquals("Pediatría", doctores.get(0).detalle());
        assertEquals("Josué Cóndor Quispe", doctores.get(1).nombre());
    }

    @Test
    void aplicaLosFiltrosDeEspecialidadDoctorYEstadoEnElRepositorio() {
        Object[] resultado = fila("Nutrición", "Josué", "Cóndor Quispe", Cita.ATENDIDA, 2);
        when(citaRepository.conteoParaReporte(DESDE, HASTA, 2, 12, Cita.ATENDIDA))
                .thenReturn(List.<Object[]>of(resultado));

        ReporteResponse reporte = servicio.citas(sesion, DESDE, HASTA, 2, 12, Cita.ATENDIDA);

        assertEquals(2, reporte.totales().total());
        assertEquals("Josué Cóndor Quispe", reporte.porDoctor().get(0).nombre());
        verify(citaRepository).conteoParaReporte(DESDE, HASTA, 2, 12, Cita.ATENDIDA);
    }

    @Test
    void cadaFilaLlevaLosIdsParaFiltrarPorElla() {
        hayCitas(fila("Pediatría", "Patricia", "Quiroz Soto", Cita.ATENDIDA, 3));

        ReporteResponse reporte = servicio.citas(sesion, DESDE, HASTA);

        FilaReporte especialidad = reporte.porEspecialidad().get(0);
        assertEquals(1, especialidad.idEspecialidad());
        assertNull(especialidad.idDoctor());
        FilaReporte doctor = reporte.porDoctor().get(0);
        assertEquals(1, doctor.idEspecialidad());
        assertEquals(10, doctor.idDoctor());
        assertNull(reporte.totales().idEspecialidad());
        assertNull(reporte.totales().idDoctor());
    }

    @Test
    void dosDoctoresConElMismoNombreNoSeMezclan() {
        hayCitas(new Object[] {1, "Pediatría", 10, "Ana", "Ríos Paz", Cita.ATENDIDA, 2L},
                new Object[] {2, "Nutrición", 20, "Ana", "Ríos Paz", Cita.ATENDIDA, 5L});

        List<FilaReporte> doctores = servicio.citas(sesion, DESDE, HASTA).porDoctor();

        assertEquals(2, doctores.size());
        assertEquals(20, doctores.get(0).idDoctor());
        assertEquals(5, doctores.get(0).total());
        assertEquals("Pediatría", doctores.get(1).detalle());
    }

    @Test
    void elEstadoSeAceptaEnMinusculasYEnBlancoEsSinFiltro() {
        when(citaRepository.conteoParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());

        servicio.citas(sesion, DESDE, HASTA, null, null, " atendida ");
        servicio.citas(sesion, DESDE, HASTA, null, null, "  ");

        verify(citaRepository).conteoParaReporte(DESDE, HASTA, null, null, Cita.ATENDIDA);
        verify(citaRepository).conteoParaReporte(DESDE, HASTA, null, null, null);
    }

    @Test
    void estadoNoPermitidoResponde400SinConsultarElRepositorio() {
        exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.citas(sesion, DESDE, HASTA, null, null, "ESTADO_INVALIDO"));

        verifyNoInteractions(citaRepository);
    }

    @Test
    void sinCitasElAusentismoEsCero() {
        hayCitas();

        ReporteResponse reporte = servicio.citas(sesion, DESDE, HASTA);

        assertEquals(0, reporte.totales().total());
        assertEquals(0, reporte.totales().ausentismo());
        assertEquals(List.of(), reporte.porDoctor());
    }

    @Test
    void sinFechasUsaElMesEnCurso() {
        LocalDate hoy = LocalDate.now();
        when(citaRepository.conteoParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());

        ReporteResponse reporte = servicio.citas(sesion, null, null);

        assertEquals(Formato.fecha(hoy.withDayOfMonth(1)), reporte.desde());
        assertEquals(Formato.fecha(hoy), reporte.hasta());
    }

    @Test
    void fechasInvertidasResponden400() {
        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.citas(sesion, HASTA, DESDE));

        assertEquals("La fecha final debe ser igual o posterior a la inicial.", error.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    void masDeUnAnioResponde400() {
        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.citas(sesion, DESDE.minusYears(2), HASTA));

        verifyNoInteractions(citaRepository);
    }

    @Test
    void sinPermisoNoConsultaLasCitas() {
        when(adminService.personal(sesion)).thenThrow(ApiException.prohibido("Sin permiso."));

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.citas(sesion, DESDE, HASTA));

        verifyNoInteractions(citaRepository);
    }

    // ----- Filtros del reporte -----

    @Test
    void losFiltrosOfrecenEspecialidadesYDoctoresActivosEnOrdenAlfabetico() {
        Especialidad pediatria = new Especialidad("Pediatría");
        pediatria.setIdEspecialidad(4);
        Especialidad nutricion = new Especialidad("Nutrición");
        nutricion.setIdEspecialidad(3);
        Doctor salazar = DatosPrueba.doctor(5, DatosPrueba.usuario(25, UsuarioSesion.DOCTOR, "87654325"));
        salazar.setEspecialidad(pediatria);
        when(especialidadRepository.findByEstadoOrderByIdEspecialidad("ACTIVO")).thenReturn(List.of(pediatria, nutricion));
        when(doctorRepository.findByEstadoOrderByIdDoctor("ACTIVO")).thenReturn(List.of(salazar));

        FiltrosReporteResponse filtros = servicio.filtros(sesion);

        assertEquals(List.of("Nutrición", "Pediatría"), filtros.especialidades().stream().map(OpcionReporte::nombre).toList());
        assertEquals(4, filtros.especialidades().get(1).id());
        OpcionReporte doctor = filtros.doctores().get(0);
        assertEquals(5, doctor.id());
        assertEquals(salazar.getUsuario().getNombres() + " " + salazar.getUsuario().getApellidos(), doctor.nombre());
        assertEquals("Pediatría", doctor.detalle());
    }

    @Test
    void sinPermisoNoSeConsultanLosFiltros() {
        when(adminService.personal(sesion)).thenThrow(ApiException.prohibido("Sin permiso."));

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.filtros(sesion));

        verifyNoInteractions(especialidadRepository, doctorRepository);
    }

    // ----- Ocupación de horarios -----

    @Test
    void calculaOcupacionDeHorariosPorEspecialidadYDoctor() {
        when(horarioRepository.conteoOcupacion(DESDE, HASTA)).thenReturn(List.of(
                filaOcupacion("Medicina General", "Rodrigo", "Mendoza Ríos", Horario.RESERVADO, 3),
                filaOcupacion("Medicina General", "Rodrigo", "Mendoza Ríos", Horario.DISPONIBLE, 1),
                filaOcupacion("Medicina General", "Rodrigo", "Mendoza Ríos", Horario.CANCELADO, 1)));

        ReporteOcupacionResponse reporte = servicio.ocupacionHorarios(sesion, DESDE, HASTA);
        FilaOcupacion totales = reporte.totales();

        assertEquals(5, totales.ofertados());
        assertEquals(3, totales.ocupados());
        assertEquals(1, totales.disponibles());
        assertEquals(1, totales.cancelados());
        assertEquals(75, totales.porcentajeOcupacion());
    }
}
