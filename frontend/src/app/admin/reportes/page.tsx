import type { Metadata } from "next"

import { ReporteCitas } from "@/components/admin/reporte-citas"
import { ReporteCitasIntegral } from "@/components/admin/reporte-citas-integral"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Reportes",
}

export default function ReportesPage() {
  return (
    <>
      <Encabezado
        titulo="Reportes de control"
        descripcion="Revisa cuántas citas se atendieron y el ausentismo por especialidad y por doctor."
      />
      <ReporteCitas />
      <ReporteCitasIntegral />
    </>
  )
}
