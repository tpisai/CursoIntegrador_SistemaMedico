package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.ReportePrescripcionDto.ReporteResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.repository.ReportePrescripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportePrescripcionServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Mock
    private AdminService adminService;
    @Mock
    private ReportePrescripcionRepository repository;
    @Mock
    private GeneradorReportesSql generadorReportesSql;

    private ReportePrescripcionService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);

    @BeforeEach
    void preparar() {
        servicio = new ReportePrescripcionService(adminService, repository, generadorReportesSql);
    }

    private LocalDateTime inicio() {
        return DESDE.atStartOfDay();
    }

    private LocalDateTime finExclusivo() {
        return HASTA.plusDays(1).atStartOfDay();
    }

    private void prepararReporte() {
        when(repository.resumen(inicio(), finExclusivo(), 4, 12))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 12L, 8L}));
        when(repository.topMedicamentos(inicio(), finExclusivo(), 4, 12))
                .thenReturn(List.<Object[]>of(new Object[]{"AMOXICILINA 500 MG", 6L}));
        when(repository.topDiagnosticos(inicio(), finExclusivo(), 4, 12))
                .thenReturn(List.<Object[]>of(new Object[]{"INFECCIÓN RESPIRATORIA", 3L}));
        when(repository.porMedico(inicio(), finExclusivo(), 4, 12))
                .thenReturn(List.<Object[]>of(new Object[]{12, "Dra. Ana Rojas", "Medicina General", 8L}));
        when(repository.tendenciaSemanal(inicio(), finExclusivo(), 4, 12))
                .thenReturn(List.<Object[]>of(new Object[]{Date.valueOf(LocalDate.of(2026, 9, 28)), 2L},
                        new Object[]{Date.valueOf(LocalDate.of(2026, 10, 5)), 6L}));
    }

    @Test
    void calculaKpisYCompletaSemanasSinRecetas() {
        prepararReporte();

        ReporteResponse reporte = servicio.reporte(sesion, DESDE, HASTA, 4, 12);

        assertEquals(10, reporte.kpis().totalAtenciones());
        assertEquals(12, reporte.kpis().totalMedicamentos());
        assertEquals(1.5, reporte.kpis().promedioMedicamentosPorReceta());
        assertEquals(80.0, reporte.kpis().porcentajeConsultasConReceta());
        assertEquals("AMOXICILINA 500 MG", reporte.topMedicamentos().get(0).nombre());
        assertEquals("INFECCIÓN RESPIRATORIA", reporte.topDiagnosticos().get(0).nombre());
        assertEquals(8, reporte.porMedico().get(0).recetas());
        assertEquals(List.of(2L, 6L, 0L, 0L, 0L), reporte.tendenciaSemanal().stream()
                .map(semana -> semana.recetas()).toList());
        verify(repository).resumen(inicio(), finExclusivo(), 4, 12);
    }

    @Test
    void devuelveCerosCuandoNoHayAtencionesNiRecetas() {
        when(repository.resumen(inicio(), finExclusivo(), null, null))
                .thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L}));

        ReporteResponse reporte = servicio.reporte(sesion, DESDE, HASTA, null, null);

        assertEquals(0, reporte.kpis().totalAtenciones());
        assertEquals(0, reporte.kpis().totalMedicamentos());
        assertEquals(0.0, reporte.kpis().promedioMedicamentosPorReceta());
        assertEquals(0.0, reporte.kpis().porcentajeConsultasConReceta());
    }

    @Test
    void fechasInvertidasResponden400AntesDeConsultarLaBase() {
        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.reporte(sesion, HASTA, DESDE, null, null));

        assertEquals("La fecha final debe ser igual o posterior a la inicial.", error.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void sinPermisoNoConsultaElReporte() {
        when(adminService.personal(sesion)).thenThrow(ApiException.prohibido("Sin permiso."));

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.reporte(sesion, DESDE, HASTA, null, null));

        verifyNoInteractions(repository);
    }

    @Test
    void csvIncluyeIndicadoresYFilasDelReporte() {
        prepararReporte();

        String csv = new String(servicio.csv(sesion, DESDE, HASTA, 4, 12).contenido(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Promedio de medicamentos por receta"));
        assertTrue(csv.contains("AMOXICILINA 500 MG"));
        assertTrue(csv.contains("Dra. Ana Rojas"));
    }

    @Test
    void generaPdfConLaPlantillaJasperDelReporte() {
        prepararReporte();
        when(generadorReportesSql.pdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        var pdf = servicio.pdf(sesion, DESDE, HASTA, 4, 12);

        assertEquals("application/pdf", pdf.tipoMime());
        assertEquals("reporte-integral-morbilidad-prescripcion-2026-10-01-a-2026-10-31.pdf", pdf.nombre());
        assertEquals(3, pdf.contenido().length);
    }
}
