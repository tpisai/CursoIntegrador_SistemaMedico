"use client"

import { useMemo } from "react"
import { es } from "react-day-picker/locale"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Card, CardContent } from "@/components/ui/card"
import { Spinner } from "@/components/ui/spinner"
import type { DisponibilidadDia } from "@/lib/api/types"
import { formatDiaSemana, formatMesAnio, parseFecha, toFechaISO } from "@/lib/format"

const LEYENDA = [
  { texto: "Con cupos", clase: "bg-accent" },
  { texto: "Seleccionado", clase: "bg-primary" },
  { texto: "Sin disponibilidad", clase: "bg-muted ring-1 ring-border" },
]

export function CalendarioDisponibilidad({
  mes,
  mesMinimo,
  mesMaximo,
  onMes,
  fecha,
  onFecha,
  disponibilidad,
  cargando,
}: {
  mes: Date
  mesMinimo: Date
  mesMaximo: Date
  onMes: (mes: Date) => void
  fecha: string | null
  onFecha: (fecha: string) => void
  disponibilidad: DisponibilidadDia[] | undefined
  cargando: boolean
}) {
  const conCupos = useMemo(() => new Set(disponibilidad?.map((d) => d.fecha)), [disponibilidad])
  const sinCuposEnElMes = !cargando && disponibilidad?.length === 0
  const mesSiguiente = new Date(mes.getFullYear(), mes.getMonth() + 1, 1)
  const hayMesSiguiente = mesSiguiente <= mesMaximo

  return (
    <Card>
      <CardContent className="flex flex-1 flex-col gap-4">
        <div className="relative">
          <Calendar
            mode="single"
            required
            locale={es}
            showOutsideDays={false}
            month={mes}
            onMonthChange={onMes}
            startMonth={mesMinimo}
            endMonth={mesMaximo}
            selected={fecha ? parseFecha(fecha) : undefined}
            onSelect={(dia) => onFecha(toFechaISO(dia))}
            disabled={(dia) => !conCupos.has(toFechaISO(dia))}
            modifiers={{ conCupos: (dia) => conCupos.has(toFechaISO(dia)) }}
            modifiersClassNames={{
              conCupos:
                "[&>button]:bg-accent [&>button]:font-semibold [&>button]:text-accent-foreground [&>button]:hover:bg-primary/20",
            }}
            formatters={{
              formatCaption: formatMesAnio,
              formatWeekdayName: formatDiaSemana,
            }}
            className="w-full bg-transparent p-0 [--cell-size:--spacing(9)]"
            classNames={{
              root: "w-full",
              months: "relative flex flex-col",
              month: "flex w-full flex-col gap-3",
              nav: "absolute top-0 right-0 flex items-center gap-1",
              month_caption: "flex h-(--cell-size) items-center",
              caption_label: "text-base font-semibold",
              weekdays: "flex gap-1.5",
              weekday: "flex-1 text-xs font-medium text-muted-foreground select-none",
              week: "mt-1.5 flex w-full gap-1.5",
              day: "relative flex-1 rounded-(--cell-radius) p-0 text-center select-none",
              day_button: "aspect-auto h-(--cell-size) text-sm",
            }}
          />
          {cargando ? (
            <div className="absolute inset-0 top-12 flex items-center justify-center bg-card/60" role="status">
              <Spinner className="size-5 text-primary" />
              <span className="sr-only">Cargando disponibilidad…</span>
            </div>
          ) : null}
        </div>

        {sinCuposEnElMes ? (
          <div className="flex flex-wrap items-center justify-between gap-3" role="status">
            <p className="text-sm text-muted-foreground">
              No quedan cupos en {formatMesAnio(mes).toLowerCase()} con este profesional.
              {hayMesSiguiente ? "" : " Prueba con otro doctor o consultorio."}
            </p>
            {hayMesSiguiente ? (
              <Button variant="outline" size="sm" onClick={() => onMes(mesSiguiente)}>
                Ver {formatMesAnio(mesSiguiente).split(" ")[0].toLowerCase()}
              </Button>
            ) : null}
          </div>
        ) : null}

        <ul className="mt-auto flex flex-wrap gap-x-5 gap-y-2 text-xs text-muted-foreground">
          {LEYENDA.map(({ texto, clase }) => (
            <li key={texto} className="flex items-center gap-1.5">
              <span className={`size-3 rounded-sm ${clase}`} aria-hidden="true" />
              {texto}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
