import type { Metadata } from "next"

import { SolicitudesCard } from "@/components/doctor/solicitudes-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Solicitudes",
}

export default function SolicitudesDoctorPage() {
  return (
    <>
      <Encabezado
        titulo="Solicitudes"
        descripcion="Sigue el estado de tus cambios de fecha y derivaciones enviados a administración."
      />
      <SolicitudesCard />
    </>
  )
}
