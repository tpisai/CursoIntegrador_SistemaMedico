import type { Metadata } from "next"

import { ReporteCitas } from "@/components/admin/reporte-citas"
import { ReporteCitasIntegral } from "@/components/admin/reporte-citas-integral"
import { ReporteOcupacionHorarios } from "@/components/admin/reporte-ocupacion-horarios"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Reportes",
}

export default function ReportesPage() {
  return (
    <>
      <Encabezado
        titulo="Reportes de control"
        descripcion="Revisa las citas atendidas, el ausentismo y la capacidad de horarios ofertados por especialidad y doctor."
      />
      <ReporteCitas />
      <ReporteCitasIntegral />
      <ReporteOcupacionHorarios />
    </>
  )
}