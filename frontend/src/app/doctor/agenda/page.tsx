import type { Metadata } from "next"

import { AgendaDoctor } from "@/components/doctor/agenda-doctor"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Agenda",
}

export default function AgendaDoctorPage() {
  return (
    <>
      <Encabezado
        titulo="Agenda"
        descripcion="Atiende a tus pacientes de hoy y revisa las citas de los días que elijas."
      />
      <AgendaDoctor />
    </>
  )
}
