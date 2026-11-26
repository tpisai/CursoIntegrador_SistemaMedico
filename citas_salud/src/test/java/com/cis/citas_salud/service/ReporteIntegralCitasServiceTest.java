package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteIntegralCitasServiceTest {

    @Mock
    private AdminService adminService;
    @Mock
    private com.cis.citas_salud.repository.ReporteIntegralCitasRepository repository;
    private ReporteIntegralCitasService servicio;
    private final com.cis.citas_salud.api.seguridad.UsuarioSesion sesion =
            new com.cis.citas_salud.api.seguridad.UsuarioSesion(30, com.cis.citas_salud.api.seguridad.UsuarioSesion.ADMINISTRADOR);

    @BeforeEach
    void preparar() {
        servicio = new ReporteIntegralCitasService(adminService, repository, org.mockito.Mockito.mock(GeneradorReportesSql.class));
    }

    @Test
    void aceptaUnPeriodoInclusivoDeHastaUnAno() {
        ReporteIntegralCitasService.validarPeriodo(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2));
    }

    @Test
    void rechazaFechasInvertidas() {
        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> ReporteIntegralCitasService.validarPeriodo(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("La fecha final debe ser igual o posterior a la inicial.", error.getMessage());
    }

    @Test
    void rechazaPeriodosMayoresAUnAno() {
        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> ReporteIntegralCitasService.validarPeriodo(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 3)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("El reporte abarca como máximo un año.", error.getMessage());
    }

    @Test
    void generaUnPdfValidoAunqueNoHayaFilas() {
        byte[] pdf = ReporteIntegralCitasService.renderizar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                List.of(), List.of(), List.of(), List.of(), List.of(), java.util.Set.of("RESUMEN"));
        assertTrue(pdf.length > 100);
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
        assertEquals('D', pdf[2]);
        assertEquals('F', pdf[3]);
    }

    @Test
    void generaGraficosConvencionalesDebajoDeSusTablasEnElPdf() {
        byte[] pdf = ReporteIntegralCitasService.renderizar(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                List.of(new Object[]{"ATENDIDA", 5L}, new Object[]{"CANCELADA", 2L}, new Object[]{"RESERVADA", 3L}),
                List.of(new Object[]{"Cardiología", "ATENDIDA", 4L}, new Object[]{"Pediatría", "CANCELADA", 2L}),
                List.of(new Object[]{"Ana", "Pérez", "Cardiología", 4L}, new Object[]{"Luis", "Gómez", "Pediatría", 2L}),
                List.of(new Object[]{"MISMO_DOCTOR", 2L}, new Object[]{"OTRO_DOCTOR", 1L}),
                List.of(new Object[]{"APROBADA", 2L}, new Object[]{"PENDIENTE", 1L}),
                java.util.Set.of("RESUMEN", "ESPECIALIDAD", "DOCTORES", "CAMBIOS", "DERIVACIONES"));

        assertTrue(pdf.length > 3_000, "el PDF debe incluir las tablas y sus recursos gráficos");
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
        assertEquals('D', pdf[2]);
        assertEquals('F', pdf[3]);
    }

    @Test
    void devuelveTodasLasSeccionesSiNoSeEspecifican() {
        assertEquals(5, ReporteIntegralCitasService.validarSecciones(null).size());
    }

    @Test
    void rechazaUnFiltroSinConsultasSeleccionadas() {
        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> ReporteIntegralCitasService.validarSecciones(List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void elFiltroDeResumenIncluyeSusMetricasDeEventosPeroOmiteOtrasSecciones() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        when(adminService.personal(sesion)).thenReturn(null);
        when(repository.integralConteoPorEstado(desde, hasta, null, null, null)).thenReturn(List.of());
        when(repository.integralEvolucionDiaria(desde, hasta, null, null, null)).thenReturn(List.of());
        when(repository.integralPorEspecialidad(desde, hasta, null, null, null)).thenReturn(List.of());
        when(repository.integralDetalle(desde, hasta, null, null, null)).thenReturn(List.of());

        servicio.datos(sesion, desde, hasta, null, null, null);

        verify(repository).integralConteoPorEstado(desde, hasta, null, null, null);
        verify(repository).integralEvolucionDiaria(desde, hasta, null, null, null);
        verify(repository).integralPorEspecialidad(desde, hasta, null, null, null);
        verify(repository).integralDetalle(desde, hasta, null, null, null);
        verify(repository, never()).conteoPorEstado(any(), any());
        verify(repository, never()).conteoPorEspecialidadYEstado(any(), any());
        verify(repository, never()).doctoresConMasCitas(any(), any());
    }

    @Test
    void rechazaUnaSeccionDesconocida() {
        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> ReporteIntegralCitasService.validarSecciones(List.of("NO_EXISTE")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }
}
