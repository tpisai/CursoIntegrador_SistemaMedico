import type { Metadata } from "next"

import { ReporteCitasIntegral } from "@/components/admin/reporte-citas-integral"

export const metadata: Metadata = {
  title: "Reporte integral de citas",
}

export default function ReporteIntegralPage() {
  return <ReporteCitasIntegral />
}
