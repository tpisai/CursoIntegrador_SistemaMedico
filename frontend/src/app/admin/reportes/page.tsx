import type { Metadata } from "next"
import { ChartColumnIcon } from "lucide-react"

import { ReporteCitas } from "@/components/admin/reporte-citas"
import { ReporteCitasIntegral } from "@/components/admin/reporte-citas-integral"
import { ReporteOcupacionHorarios } from "@/components/admin/reporte-ocupacion-horarios"
import { HeroSeccion } from "@/components/portal/hero-seccion"

export const metadata: Metadata = {
  title: "Reportes",
}

export default function ReportesPage() {
  return (
    <>
      <HeroSeccion
        etiqueta="Administración"
        titulo="Reportes de control"
        descripcion="Revisa las citas atendidas, el ausentismo y la capacidad de horarios ofertados por especialidad y doctor. Descarga el reporte integral en PDF."
        Icono={ChartColumnIcon}
      />
      <ReporteCitas />
      <ReporteCitasIntegral />
      <ReporteOcupacionHorarios />
    </>
  )
}
