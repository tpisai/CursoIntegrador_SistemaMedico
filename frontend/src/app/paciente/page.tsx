import type { Metadata } from "next"
import Link from "next/link"
import { CalendarPlusIcon, FileTextIcon, MegaphoneIcon, TicketIcon } from "lucide-react"

import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { HistorialCard } from "@/components/citas/historial-card"
import { MisCitasCard } from "@/components/citas/mis-citas-card"
import { ProximaCita } from "@/components/citas/proxima-cita"
import { Encabezado } from "@/components/portal/encabezado"
import { Saludo } from "@/components/portal/saludo"

export const metadata: Metadata = {
  title: "Inicio",
}

const ACCESOS = [
  { href: "/paciente/agendar", titulo: "Agendar cita", texto: "Elige especialidad, doctor y horario.", Icono: CalendarPlusIcon },
  { href: "/paciente/citas", titulo: "Mis citas", texto: "Ve tu ticket o cancela una cita.", Icono: TicketIcon },
  { href: "/paciente/historial", titulo: "Historial médico", texto: "Revisa tus atenciones y documentos.", Icono: FileTextIcon },
  { href: "/paciente/comunicacion", titulo: "Campañas de salud", texto: "Inscríbete en campañas gratuitas.", Icono: MegaphoneIcon },
]

export default function InicioPacientePage() {
  return (
    <>
      <Encabezado titulo={<Saludo />} descripcion="¿Qué necesitas hacer hoy?" />
      <ProximaCita />
      <nav aria-label="Accesos rápidos">
        <ul className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {ACCESOS.map(({ href, titulo, texto, Icono }) => (
            <li key={href}>
              <Link href={href} className="group block h-full rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
                <Card className="h-full transition-colors group-hover:bg-secondary">
                  <CardHeader className="gap-1.5">
                    <span className="mb-2 flex size-11 items-center justify-center rounded-lg bg-secondary text-primary group-hover:bg-card">
                      <Icono className="size-5" aria-hidden="true" />
                    </span>
                    <CardTitle>{titulo}</CardTitle>
                    <CardDescription>{texto}</CardDescription>
                  </CardHeader>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      <div className="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_400px]">
        <MisCitasCard conTicket={false} />
        <HistorialCard />
      </div>
    </>
  )
}
