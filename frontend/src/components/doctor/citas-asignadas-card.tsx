"use client"

import { CalendarCheckIcon, CircleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { useCitasDelDia } from "@/hooks/use-doctor"
import { mensajeDeError } from "@/lib/api/http"

// RF-14: citas ya asignadas al doctor para un día (pantalla 03 del prototipo).
export function CitasAsignadasCard({
  fecha,
  descripcion,
  accion,
}: {
  fecha: string
  descripcion: string
  accion?: React.ReactNode
}) {
  const { data: citas, error, isLoading } = useCitasDelDia(fecha)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Citas pendientes asignadas</CardTitle>
        <CardDescription>{descripcion}</CardDescription>
        {accion ? <CardAction>{accion}</CardAction> : null}
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 4 }, (_, i) => (
              <Skeleton key={i} className="h-14" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !citas?.length ? (
          <Empty className="border border-dashed p-8">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <CalendarCheckIcon />
              </EmptyMedia>
              <EmptyTitle>Sin citas pendientes</EmptyTitle>
              <EmptyDescription>Cuando un paciente reserve un turno contigo aparecerá aquí.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <ul className="flex flex-col">
            {citas.map((cita, i) => (
              <li key={cita.idCita} className="flex flex-col">
                {i > 0 ? <Separator /> : null}
                <div className="flex items-center gap-4 py-3">
                  <span className="flex h-10 w-16 shrink-0 items-center justify-center rounded-lg bg-secondary text-sm font-semibold text-secondary-foreground">
                    {cita.horaInicio}
                  </span>
                  <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="truncate text-sm font-medium">
                      {cita.paciente} · DNI {cita.dniPaciente}
                    </span>
                    <span className="truncate text-xs text-muted-foreground">
                      {cita.motivo ?? "Sin motivo registrado"} · Ticket {cita.numeroTicket} · {cita.consultorio}
                    </span>
                  </div>
                  <EstadoCitaBadge estado={cita.estado} vista="doctor" />
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
