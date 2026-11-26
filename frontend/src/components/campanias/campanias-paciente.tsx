"use client"

import { useState, useTransition } from "react"
import { BellIcon, CalendarIcon, CircleAlertIcon, ClockIcon, DownloadIcon, MapPinIcon, MegaphoneIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { EstadoCampaniaBadge } from "@/components/campanias/estado-campania-badge"
import { bajarComprobante, type EstadoInscripcion, InscripcionDialog } from "@/components/campanias/inscripcion-dialog"
import { useCampanias } from "@/hooks/use-campanias"
import { mensajeDeError } from "@/lib/api/http"
import type { Campania } from "@/lib/api/types"
import { formatRango, parseFecha } from "@/lib/format"
import { cn } from "@/lib/utils"

const DIAS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"]
const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]

// Un tono por tarjeta para que la cuadrícula no se vea repetida.
const TONOS = [
  "from-cyan-700 to-teal-500",
  "from-violet-700 to-indigo-500",
  "from-rose-600 to-orange-400",
  "from-emerald-700 to-lime-500",
  "from-blue-800 to-sky-500",
  "from-fuchsia-700 to-pink-500",
]

function TarjetaCampania({
  campania,
  tono,
  onInscribir,
}: {
  campania: Campania
  tono: string
  onInscribir: () => void
}) {
  const [descargando, startDescarga] = useTransition()
  const sinCupos = campania.cuposDisponibles === 0
  const inicio = parseFecha(campania.fechaInicio)
  const ocupados = campania.cupos - campania.cuposDisponibles
  const porcentaje = campania.cupos ? Math.round((ocupados / campania.cupos) * 100) : 0
  const ultimos = !sinCupos && campania.cuposDisponibles <= Math.max(5, campania.cupos * 0.15)

  return (
    <Card className="h-full gap-0 py-0 transition-[translate,box-shadow] duration-200 hover:-translate-y-1 hover:shadow-[0_16px_40px_rgb(14_60_90/0.14)]">
      <div className={cn("relative flex h-32 items-start justify-between overflow-hidden bg-linear-to-br p-4 text-white", tono)}>
        <span aria-hidden="true" className="absolute -right-8 -bottom-12 size-36 rounded-full bg-white/15" />
        <MegaphoneIcon aria-hidden="true" className="absolute right-5 bottom-4 size-14 opacity-25" strokeWidth={1.5} />
        <div className="relative flex flex-col items-center rounded-xl bg-white px-3 py-1.5 leading-none text-slate-900 shadow-md">
          <span className="text-[10px] font-bold text-slate-500 uppercase">{DIAS[inicio.getDay()]}</span>
          <span className="text-2xl font-bold tabular-nums">{inicio.getDate()}</span>
          <span className="text-[11px] font-semibold uppercase">{MESES[inicio.getMonth()]}</span>
        </div>
        <div className="relative flex flex-col items-end gap-1.5">
          <EstadoCampaniaBadge estado={campania.estado} />
          {ultimos ? (
            <span className="rounded-full bg-lime px-2 py-0.5 text-[11px] font-bold text-lime-foreground">
              ¡Últimos cupos!
            </span>
          ) : null}
        </div>
      </div>

      <CardContent className="flex flex-1 flex-col gap-3 py-5 lg:py-6">
        <h3 className="text-lg leading-snug font-bold">{campania.titulo}</h3>
        <ul className="flex flex-col gap-1.5 text-[13px] text-muted-foreground">
          <li className="flex items-center gap-2">
            <CalendarIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
            {formatRango(campania.fechaInicio, campania.fechaFin)}
          </li>
          {campania.hora ? (
            <li className="flex items-center gap-2">
              <ClockIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
              Desde las {campania.hora}
            </li>
          ) : null}
          {campania.lugar ? (
            <li className="flex items-center gap-2">
              <MapPinIcon className="size-4 shrink-0 text-primary" aria-hidden="true" />
              {campania.lugar}
            </li>
          ) : null}
        </ul>
        {campania.descripcion ? (
          <p className="line-clamp-4 text-sm leading-relaxed">{campania.descripcion}</p>
        ) : null}

        <div className="mt-auto flex flex-col gap-1.5 pt-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-medium text-muted-foreground">
              {sinCupos ? "Sin cupos disponibles" : `Quedan ${campania.cuposDisponibles} de ${campania.cupos} cupos`}
            </span>
            <span className="font-semibold tabular-nums">{porcentaje} %</span>
          </div>
          <div
            className="h-2 overflow-hidden rounded-full bg-muted"
            role="progressbar"
            aria-label="Cupos ocupados"
            aria-valuenow={porcentaje}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div
              className={cn("h-full rounded-full", sinCupos ? "bg-muted-foreground" : ultimos ? "bg-warning-foreground" : "bg-primary")}
              style={{ width: `${porcentaje}%` }}
            />
          </div>
        </div>

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
          <Button size="xl" className="w-full rounded-full" disabled={sinCupos} onClick={onInscribir}>
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
    <section aria-labelledby="campanias-disponibles" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id="campanias-disponibles" className="text-2xl font-bold tracking-tight">
            Campañas disponibles
          </h2>
          <p className="text-muted-foreground">Gratuitas y con cupos limitados: inscríbete y descarga tu comprobante.</p>
        </div>
        {campanias?.length ? (
          <Badge variant="secondary" className="h-7 px-3 text-sm">
            {campanias.length} {campanias.length === 1 ? "campaña" : "campañas"}
          </Badge>
        ) : null}
      </div>

      <Alert variant="brand" role="note">
        <BellIcon />
        <AlertDescription>
          Cuando el centro de salud publique una campaña nueva te avisaremos en la campana de notificaciones.
        </AlertDescription>
      </Alert>

      {isLoading ? (
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-[420px] rounded-xl" />
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
          {campanias.map((c, i) => (
            <li key={c.idCampania}>
              <TarjetaCampania
                campania={c}
                tono={TONOS[i % TONOS.length]}
                onInscribir={() => setInscripcion({ campania: c, abierto: true })}
              />
            </li>
          ))}
        </ul>
      )}

      <InscripcionDialog estado={inscripcion} onCerrar={() => setInscripcion((i) => i && { ...i, abierto: false })} />
    </section>
  )
}
