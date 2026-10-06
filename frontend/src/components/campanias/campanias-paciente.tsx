"use client"

import { useState, useTransition } from "react"
import { BellIcon, CalendarIcon, CircleAlertIcon, DownloadIcon, MapPinIcon, MegaphoneIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { EstadoCampaniaBadge } from "@/components/campanias/estado-campania-badge"
import { bajarComprobante, type EstadoInscripcion, InscripcionDialog } from "@/components/campanias/inscripcion-dialog"
import { useCampanias } from "@/hooks/use-campanias"
import { mensajeDeError } from "@/lib/api/http"
import type { Campania } from "@/lib/api/types"
import { formatRango } from "@/lib/format"

function TarjetaCampania({ campania, onInscribir }: { campania: Campania; onInscribir: () => void }) {
  const [descargando, startDescarga] = useTransition()
  const sinCupos = campania.cuposDisponibles === 0

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <span className="flex items-center gap-1.5 text-[11px] font-semibold tracking-wider text-secondary-foreground uppercase">
            <MegaphoneIcon className="size-3.5" aria-hidden="true" />
            Campaña
          </span>
          <EstadoCampaniaBadge estado={campania.estado} />
        </div>
        <CardTitle className="text-lg font-semibold">{campania.titulo}</CardTitle>
        <CardDescription className="flex flex-col gap-1">
          <span className="flex items-center gap-1.5">
            <CalendarIcon className="size-3.5 shrink-0" aria-hidden="true" />
            {formatRango(campania.fechaInicio, campania.fechaFin, campania.hora)}
          </span>
          {campania.lugar ? (
            <span className="flex items-center gap-1.5">
              <MapPinIcon className="size-3.5 shrink-0" aria-hidden="true" />
              {campania.lugar}
            </span>
          ) : null}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-3 text-sm">
        {campania.descripcion ? <p className="leading-relaxed">{campania.descripcion}</p> : null}
        <p className="mt-auto text-xs text-muted-foreground">
          {sinCupos ? "Sin cupos disponibles" : `Quedan ${campania.cuposDisponibles} de ${campania.cupos} cupos`}
        </p>
        {campania.idInscripcion && campania.codigoInscripcion ? (
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="success">Inscrito · {campania.codigoInscripcion}</Badge>
            <Button
              variant="outline"
              size="sm"
              disabled={descargando}
              onClick={() =>
                startDescarga(() => bajarComprobante(campania.idInscripcion!, campania.codigoInscripcion!))
              }
            >
              {descargando ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
              Comprobante
            </Button>
          </div>
        ) : (
          <Button size="lg" className="w-full" disabled={sinCupos} onClick={onInscribir}>
            {sinCupos ? "Sin cupos" : "Inscribirme a la campaña"}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}

// Pantalla 05 · Comunicación y Campañas del prototipo (lado del paciente).
export function CampaniasPaciente() {
  const { data: campanias, error, isLoading } = useCampanias()
  const [inscripcion, setInscripcion] = useState<EstadoInscripcion | null>(null)

  return (
    <>
      <Alert variant="brand" role="note">
        <BellIcon />
        <AlertDescription>
          Cuando el centro de salud publique una campaña nueva te avisaremos en la campana de notificaciones.
        </AlertDescription>
      </Alert>

      {isLoading ? (
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-64" />
          ))}
        </div>
      ) : error ? (
        <Alert variant="destructive">
          <CircleAlertIcon />
          <AlertDescription>{mensajeDeError(error)}</AlertDescription>
        </Alert>
      ) : !campanias?.length ? (
        <Card>
          <CardContent>
            <Empty className="py-12">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <MegaphoneIcon />
                </EmptyMedia>
                <EmptyTitle>No hay campañas por ahora</EmptyTitle>
                <EmptyDescription>Te avisaremos cuando el centro de salud publique una nueva.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          </CardContent>
        </Card>
      ) : (
        <ul className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {campanias.map((c) => (
            <li key={c.idCampania}>
              <TarjetaCampania campania={c} onInscribir={() => setInscripcion({ campania: c, abierto: true })} />
            </li>
          ))}
        </ul>
      )}

      <InscripcionDialog estado={inscripcion} onCerrar={() => setInscripcion((i) => i && { ...i, abierto: false })} />
    </>
  )
}
