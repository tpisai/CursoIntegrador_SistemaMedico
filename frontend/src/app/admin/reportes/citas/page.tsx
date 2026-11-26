import type { Metadata } from "next"

import { ReporteCitas } from "@/components/admin/reporte-citas"

export const metadata: Metadata = {
  title: "Reporte de citas y ausentismo",
}

export default function ReporteCitasPage() {
  return <ReporteCitas />
}
