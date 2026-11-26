import type { Metadata } from "next"

import { ReportePrescripcionClinica } from "@/components/admin/reporte-prescripcion-clinica"

export const metadata: Metadata = {
  title: "Morbilidad y prescripción",
}

export default function ReportePrescripcionPage() {
  return <ReportePrescripcionClinica />
}
