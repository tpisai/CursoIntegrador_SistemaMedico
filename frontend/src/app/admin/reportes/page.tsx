import type { Metadata } from "next"

import { ReporteCitas } from "@/components/admin/reporte-citas"
import { ReporteCitasIntegral } from "@/components/admin/reporte-citas-integral"
import { ReporteOcupacionHorarios } from "@/components/admin/reporte-ocupacion-horarios"
import { ReportePrescripcionClinica } from "@/components/admin/reporte-prescripcion-clinica"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Reportes",
}

export default function ReportesPage() {
  return (
    <>
      <Encabezado
        titulo="Reportes de control"
        descripcion="Analiza citas, ausentismo, ocupación de horarios, morbilidad y prescripción médica."
      />
      <ReporteCitas />
      <ReporteCitasIntegral />
      <ReporteOcupacionHorarios />
      <ReportePrescripcionClinica />
    </>
  )
}