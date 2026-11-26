import { redirect } from "next/navigation"

import { REPORTE_INICIAL } from "@/lib/admin/reportes"

// /admin/reportes abre el primer reporte del menú.
export default function ReportesPage() {
  redirect(REPORTE_INICIAL)
}
