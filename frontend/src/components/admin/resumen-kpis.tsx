"use client"

import { CalendarDaysIcon, ClipboardListIcon, MegaphoneIcon, UserXIcon } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useResumenAdmin } from "@/hooks/use-admin"

// Indicadores de la cabecera del panel de administración (pantalla 04 del prototipo).
export function ResumenKpis() {
  const { data } = useResumenAdmin()

  const indicadores = [
    { icono: CalendarDaysIcon, valor: data?.citasHoy, texto: "Citas programadas hoy" },
    { icono: ClipboardListIcon, valor: data?.solicitudesPendientes, texto: "Solicitudes pendientes" },
    { icono: MegaphoneIcon, valor: data?.campaniasVigentes, texto: "Campañas vigentes" },
    { icono: UserXIcon, valor: data ? `${data.ausentismoMes} %` : undefined, texto: "Ausentismo del mes" },
  ]

  return (
    <ul className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {indicadores.map(({ icono: Icono, valor, texto }) => (
        <li key={texto}>
          <Card size="sm">
            <CardContent className="flex items-center gap-3">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
                <Icono className="size-5" aria-hidden="true" />
              </span>
              <div className="flex min-w-0 flex-col">
                {valor === undefined ? (
                  <Skeleton className="h-7 w-12" />
                ) : (
                  <span className="text-2xl font-bold tracking-tight">{valor}</span>
                )}
                <span className="text-xs leading-tight text-muted-foreground">{texto}</span>
              </div>
            </CardContent>
          </Card>
        </li>
      ))}
    </ul>
  )
}
