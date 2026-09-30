import type { Metadata } from "next"
import Link from "next/link"
import { CalendarPlusIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { HistorialCard } from "@/components/citas/historial-card"
import { MisCitasCard } from "@/components/citas/mis-citas-card"
import { Encabezado } from "@/components/portal/encabezado"
import { Saludo } from "@/components/portal/saludo"

export const metadata: Metadata = {
  title: "Inicio",
}

export default function InicioPacientePage() {
  return (
    <>
      <Encabezado titulo={<Saludo />} descripcion="Este es el resumen de tus citas y documentos médicos.">
        <Button asChild size="xl">
          <Link href="/paciente/agendar">
            <CalendarPlusIcon data-icon="inline-start" />
            Agendar nueva cita
          </Link>
        </Button>
      </Encabezado>
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_430px]">
        <MisCitasCard />
        <HistorialCard />
      </div>
    </>
  )
}
