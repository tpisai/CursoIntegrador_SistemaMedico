"use client"

import { useState } from "react"
import { CalendarCheckIcon, CircleAlertIcon, StethoscopeIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { RegistrarAtencionDialog } from "@/components/doctor/registrar-atencion-dialog"
import { useCitasDelDia } from "@/hooks/use-doctor"
import { mensajeDeError } from "@/lib/api/http"
import type { CitaAsignada } from "@/lib/api/types"
import { toFechaISO } from "@/lib/format"

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
  const [hoy] = useState(() => toFechaISO(new Date()))
  // Se guarda la cita aparte de "abierto" para que el contenido no desaparezca mientras el diálogo se cierra.
  const [atender, setAtender] = useState<{ cita: CitaAsignada; abierto: boolean } | null>(null)

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
                {/* En móvil el estado y el botón bajan a una segunda línea para no cortar el nombre. */}
                <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3 sm:flex-nowrap">
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
                  <div className="flex w-full items-center justify-between gap-4 ps-20 sm:w-auto sm:ps-0">
                    <EstadoCitaBadge estado={cita.estado} vista="doctor" />
                    {cita.fecha <= hoy ? (
                      <Button size="sm" onClick={() => setAtender({ cita, abierto: true })}>
                        <StethoscopeIcon data-icon="inline-start" />
                        Atender
                        <span className="sr-only"> a {cita.paciente}</span>
                      </Button>
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <RegistrarAtencionDialog
        cita={atender?.cita ?? null}
        abierto={atender?.abierto ?? false}
        onCerrar={() => setAtender((a) => a && { ...a, abierto: false })}
      />
    </Card>
  )
}
