import type { Metadata } from "next"

import { ReporteOcupacionHorarios } from "@/components/admin/reporte-ocupacion-horarios"

export const metadata: Metadata = {
  title: "Ocupación de horarios",
}

export default function ReporteHorariosPage() {
  return <ReporteOcupacionHorarios />
}
