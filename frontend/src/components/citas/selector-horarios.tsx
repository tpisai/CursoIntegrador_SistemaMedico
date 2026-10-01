"use client"

import { CalendarSearchIcon, TriangleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import type { Horario } from "@/lib/api/types"
import { formatDiaCorto } from "@/lib/format"

export function SelectorHorarios({
  fecha,
  horarios,
  cargando,
  idHorario,
  onHorario,
  onConfirmar,
  reservando,
}: {
  fecha: string | null
  horarios: Horario[] | undefined
  cargando: boolean
  idHorario: number | null
  onHorario: (idHorario: number | null) => void
  onConfirmar: (horario: Horario) => void
  reservando: boolean
}) {
  const elegido = horarios?.find((h) => h.idHorario === idHorario)
  const elegidoDisponible = elegido?.estado === "DISPONIBLE" ? elegido : undefined
  // Otro paciente lo reservó mientras este lo tenía seleccionado.
  const ganadoPorOtro = elegido !== undefined && !elegidoDisponible

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">
          {fecha ? `Horarios · ${formatDiaCorto(fecha)}` : "Horarios"}
        </CardTitle>
        <CardDescription>
          {fecha ? "Selecciona un horario disponible:" : "Elige un día con cupos en el calendario."}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-1 flex-col gap-4">
        {!fecha ? (
          <Empty className="flex-1 border border-dashed p-6">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <CalendarSearchIcon />
              </EmptyMedia>
              <EmptyTitle>Aún no eliges un día</EmptyTitle>
              <EmptyDescription>Los días resaltados en el calendario tienen cupos libres.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : cargando ? (
          <div className="grid grid-cols-2 gap-2">
            {Array.from({ length: 8 }, (_, i) => (
              <Skeleton key={i} className="h-9" />
            ))}
          </div>
        ) : horarios?.length ? (
          <ToggleGroup
            type="single"
            variant="slot"
            size="lg"
            spacing={2}
            aria-label="Horarios disponibles"
            className="grid w-full grid-cols-2"
            value={elegidoDisponible ? String(elegidoDisponible.idHorario) : ""}
            onValueChange={(valor) => onHorario(valor ? Number(valor) : null)}
          >
            {horarios.map((h) => {
              const libre = h.estado === "DISPONIBLE"
              return (
                <ToggleGroupItem
                  key={h.idHorario}
                  value={String(h.idHorario)}
                  disabled={!libre}
                  aria-label={libre ? `${h.horaInicio} a ${h.horaFin}` : `${h.horaInicio}, no disponible`}
                  className="w-full"
                >
                  {h.horaInicio}
                </ToggleGroupItem>
              )
            })}
          </ToggleGroup>
        ) : (
          <p className="text-sm text-muted-foreground">Este profesional no atiende ese día.</p>
        )}

        {ganadoPorOtro ? (
          <Alert variant="destructive">
            <TriangleAlertIcon />
            <AlertDescription>
              El horario de las {elegido.horaInicio} acaba de ser reservado por otro paciente. Elige otro.
            </AlertDescription>
          </Alert>
        ) : null}

        <div className="mt-auto flex flex-col gap-3">
          <p className="text-xs leading-relaxed text-muted-foreground">
            Al confirmar, el cupo se bloquea en tiempo real y se genera tu ticket de atención.
          </p>
          <Button
            size="xl"
            className="w-full"
            disabled={!elegidoDisponible || reservando}
            onClick={() => elegidoDisponible && onConfirmar(elegidoDisponible)}
          >
            {reservando ? <Spinner data-icon="inline-start" /> : null}
            Confirmar cita
          </Button>
          <Button variant="ghost" disabled={idHorario === null || reservando} onClick={() => onHorario(null)}>
            Cancelar selección
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
