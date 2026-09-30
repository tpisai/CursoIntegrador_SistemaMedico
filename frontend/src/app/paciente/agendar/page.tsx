import type { Metadata } from "next"

import { HistorialCard } from "@/components/citas/historial-card"
import { MisCitasCard } from "@/components/citas/mis-citas-card"
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
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_430px]">
        <MisCitasCard />
        <HistorialCard />
      </div>
    </>
  )
}
