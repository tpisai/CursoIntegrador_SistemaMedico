package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.FilaReporte;
import com.cis.citas_salud.api.dto.AdminDto.ReporteResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.repository.CitaRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** EDT 4.8: reporte de control de citas y ausentismo. */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Mock
    private AdminService adminService;
    @Mock
    private CitaRepository citaRepository;

    private ReporteService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(adminService, citaRepository);
    }

    private static Object[] fila(String especialidad, String nombres, String apellidos, String estado, long citas) {
        return new Object[] {especialidad, nombres, apellidos, estado, citas};
    }

    private void hayCitas(Object[]... filas) {
        when(citaRepository.conteoParaReporte(DESDE, HASTA)).thenReturn(List.of(filas));
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
        when(citaRepository.conteoParaReporte(any(), any())).thenReturn(List.of());

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
}
