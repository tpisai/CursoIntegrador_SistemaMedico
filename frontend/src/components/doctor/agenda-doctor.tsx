"use client"

import { useMemo, useState } from "react"
import Link from "next/link"
import { CalendarCheckIcon, CircleAlertIcon, StethoscopeIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { type Rango, SelectorRango } from "@/components/doctor/selector-rango"
import { useAgenda } from "@/hooks/use-consulta"
import { mensajeDeError } from "@/lib/api/http"
import type { CitaAsignada } from "@/lib/api/types"
import { formatFechaLarga, formatRango, parseFecha, toFechaISO } from "@/lib/format"

function sumarDias(fecha: string, dias: number) {
  const d = parseFecha(fecha)
  d.setDate(d.getDate() + dias)
  return toFechaISO(d)
}

const estaPendiente = (cita: CitaAsignada) => cita.estado === "RESERVADA" || cita.estado === "CONFIRMADA"

function plural(cantidad: number, uno: string, varios: string) {
  return `${cantidad} ${cantidad === 1 ? uno : varios}`
}

function Accion({ cita, hoy }: { cita: CitaAsignada; hoy: string }) {
  const ruta = `/doctor/consulta/${cita.idCita}`
  if (estaPendiente(cita) && cita.fecha <= hoy) {
    return (
      <Button asChild className="h-11 sm:h-8">
        <Link href={ruta}>
          <StethoscopeIcon data-icon="inline-start" />
          Atender
          <span className="sr-only"> a {cita.paciente}</span>
        </Link>
      </Button>
    )
  }
  return (
    <Button asChild variant="outline" className="h-11 sm:h-8">
      <Link href={ruta}>
        {cita.estado === "ATENDIDA" ? "Ver atención" : "Ver historia"}
        <span className="sr-only"> de {cita.paciente}</span>
      </Link>
    </Button>
  )
}

// RF-14: citas del doctor de hoy, de la semana o del rango de fechas que elija.
export function AgendaDoctor() {
  const [hoy] = useState(() => toFechaISO(new Date()))
  const [rango, setRango] = useState<Rango>({ desde: hoy, hasta: hoy })
  const { data: citas, error, isLoading } = useAgenda(rango.desde, rango.hasta)

  const semana = sumarDias(hoy, 6)
  const atajo =
    rango.desde !== hoy ? "" : rango.hasta === hoy ? "hoy" : rango.hasta === semana ? "semana" : ""

  const porDia = useMemo(() => {
    const dias = new Map<string, CitaAsignada[]>()
    for (const cita of citas ?? []) dias.set(cita.fecha, [...(dias.get(cita.fecha) ?? []), cita])
    return [...dias]
  }, [citas])

  const pendientes = citas?.filter(estaPendiente).length ?? 0
  const atendidas = citas?.filter((c) => c.estado === "ATENDIDA").length ?? 0
  const resumen = citas
    ? `${plural(pendientes, "pendiente", "pendientes")} y ${plural(atendidas, "atendida", "atendidas")}`
    : "Cargando tus citas…"
  const unSoloDia = rango.desde === rango.hasta

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          {unSoloDia
            ? `${rango.desde === hoy ? "Hoy, " : ""}${formatFechaLarga(rango.desde).toLowerCase()}`
            : formatRango(rango.desde, rango.hasta)}
        </CardTitle>
        <CardDescription role="status">{resumen}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <ToggleGroup
            type="single"
            variant="outline"
            size="lg"
            spacing={2}
            aria-label="Fechas de la agenda"
            value={atajo}
            onValueChange={(valor) => {
              if (valor === "hoy") setRango({ desde: hoy, hasta: hoy })
              if (valor === "semana") setRango({ desde: hoy, hasta: semana })
            }}
          >
            <ToggleGroupItem value="hoy" className="h-11 px-4">
              Hoy
            </ToggleGroupItem>
            <ToggleGroupItem value="semana" className="h-11 px-4">
              Próximos 7 días
            </ToggleGroupItem>
          </ToggleGroup>
          <SelectorRango rango={rango} onCambio={setRango} />
        </div>

        {isLoading && !citas ? (
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
              <EmptyTitle>Sin citas en estas fechas</EmptyTitle>
              <EmptyDescription>Cuando un paciente reserve un turno contigo aparecerá aquí.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="flex flex-col gap-6">
            {porDia.map(([fecha, delDia]) => (
              <section key={fecha} aria-labelledby={`agenda-${fecha}`} className="flex flex-col">
                {unSoloDia ? (
                  <h3 id={`agenda-${fecha}`} className="sr-only">
                    Citas del {formatFechaLarga(fecha).toLowerCase()}
                  </h3>
                ) : (
                  <h3 id={`agenda-${fecha}`} className="flex items-baseline justify-between gap-3 pb-1 font-semibold">
                    {fecha === hoy ? `Hoy, ${formatFechaLarga(fecha).toLowerCase()}` : formatFechaLarga(fecha)}
                    <span className="text-sm font-normal text-muted-foreground">
                      {plural(delDia.length, "cita", "citas")}
                    </span>
                  </h3>
                )}
                <ul className="flex flex-col">
                  {delDia.map((cita, i) => (
                    <li key={cita.idCita} className="flex flex-col">
                      {i > 0 || !unSoloDia ? <Separator /> : null}
                      {/* En móvil el estado y el botón bajan a una segunda línea para no cortar el nombre. */}
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3 sm:flex-nowrap">
                        <span className="flex h-10 w-16 shrink-0 items-center justify-center rounded-lg bg-secondary text-sm font-semibold text-secondary-foreground">
                          {cita.horaInicio}
                        </span>
                        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                          <span className="truncate font-medium">
                            {cita.paciente} · DNI {cita.dniPaciente}
                          </span>
                          <span className="truncate text-sm text-muted-foreground">
                            {cita.motivo ?? "Sin motivo registrado"} · Ticket {cita.numeroTicket} · {cita.consultorio}
                          </span>
                        </div>
                        <div className="flex w-full items-center justify-between gap-4 ps-20 sm:w-auto sm:ps-0">
                          <EstadoCitaBadge estado={cita.estado} vista="doctor" />
                          <Accion cita={cita} hoy={hoy} />
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
