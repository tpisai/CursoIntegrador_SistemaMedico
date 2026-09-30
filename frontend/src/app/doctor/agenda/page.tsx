import type { Metadata } from "next"

import { AgendaDelDia } from "@/components/doctor/agenda-del-dia"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Mi agenda",
}

export default function AgendaDoctorPage() {
  return (
    <>
      <Encabezado titulo="Mi agenda" descripcion="Revisa las citas asignadas de cualquier día." />
      <div className="max-w-4xl">
        <AgendaDelDia navegable />
      </div>
    </>
  )
}
