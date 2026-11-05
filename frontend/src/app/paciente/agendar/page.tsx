import type { Metadata } from "next"

import { ReservaCita } from "@/components/citas/reserva-cita"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Agendar cita",
}

export default function AgendarPage() {
  return (
    <>
      <Encabezado
        titulo="Agendar cita médica"
        descripcion="Consulta la disponibilidad de citas en tiempo real por especialidad, doctor y horario."
      />
      <ReservaCita />
    </>
  )
}
