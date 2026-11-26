package com.cis.citas_salud.api.dto;

import java.util.List;

public final class ReportePrescripcionDto {

    private ReportePrescripcionDto() {
    }

    public record Kpis(
            long totalAtenciones,
            long totalMedicamentos,
            double promedioMedicamentosPorReceta,
            double porcentajeConsultasConReceta) {
    }

    public record MedicamentoFrecuente(String nombre, long cantidad) {
    }

    public record DiagnosticoFrecuente(String nombre, long atenciones) {
    }

    public record PrescripcionPorMedico(Integer idDoctor, String doctor, String especialidad, long recetas) {
    }

    public record TendenciaSemanal(String semana, long recetas) {
    }

    public record ReporteResponse(
            String desde,
            String hasta,
            Kpis kpis,
            List<MedicamentoFrecuente> topMedicamentos,
            List<DiagnosticoFrecuente> topDiagnosticos,
            List<PrescripcionPorMedico> porMedico,
            List<TendenciaSemanal> tendenciaSemanal) {
    }
}
