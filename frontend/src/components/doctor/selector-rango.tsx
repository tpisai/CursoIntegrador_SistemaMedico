"use client"

import { useState } from "react"
import { CalendarDaysIcon } from "lucide-react"
import type { DateRange } from "react-day-picker"
import { es } from "react-day-picker/locale"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { formatDiaCorto, formatDiaSemana, formatMesAnio, formatRango, parseFecha, toFechaISO } from "@/lib/format"

export const MAX_DIAS_AGENDA = 31
export type Rango = { desde: string; hasta: string }

const UN_DIA = 86_400_000

/** Calendario para elegir de qué día a qué día ver la agenda (por ejemplo, del 10 al 12 de octubre). */
export function SelectorRango({ rango, onCambio }: { rango: Rango; onCambio: (rango: Rango) => void }) {
  const [abierto, setAbierto] = useState(false)
  const [borrador, setBorrador] = useState<DateRange | undefined>()

  function cambiarApertura(abrir: boolean) {
    // Cada vez que se abre parte de las fechas que se están viendo.
    if (abrir) setBorrador({ from: parseFecha(rango.desde), to: parseFecha(rango.hasta) })
    setAbierto(abrir)
  }

  const desde = borrador?.from
  const hasta = borrador?.to
  const dias = desde && hasta ? Math.round((hasta.getTime() - desde.getTime()) / UN_DIA) + 1 : 0
  const excede = dias > MAX_DIAS_AGENDA
  const ayuda = !desde
    ? "Elige el primer día."
    : !hasta
      ? "Elige el último día."
      : excede
        ? `Elige como máximo ${MAX_DIAS_AGENDA} días.`
        : dias === 1
          ? formatDiaCorto(toFechaISO(desde))
          : `${dias} días: ${formatRango(toFechaISO(desde), toFechaISO(hasta))}`

  function aplicar() {
    if (!desde || !hasta || excede) return
    onCambio({ desde: toFechaISO(desde), hasta: toFechaISO(hasta) })
    setAbierto(false)
  }

  return (
    <Popover open={abierto} onOpenChange={cambiarApertura}>
      <PopoverTrigger asChild>
        <Button variant="outline" className="h-11 justify-start px-3.5 font-normal">
          <CalendarDaysIcon data-icon="inline-start" />
          {formatRango(rango.desde, rango.hasta)}
          <span className="sr-only">. Elegir otras fechas</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-auto p-0">
        <Calendar
          mode="range"
          locale={es}
          selected={borrador}
          onSelect={setBorrador}
          defaultMonth={desde}
          formatters={{ formatCaption: formatMesAnio, formatWeekdayName: formatDiaSemana }}
          className="p-3 [--cell-size:--spacing(10)]"
        />
        <div className="flex items-center justify-between gap-4 border-t p-3">
          <p className={excede ? "text-sm text-destructive" : "text-sm text-muted-foreground"} role="status">
            {ayuda}
          </p>
          <Button size="lg" disabled={!desde || !hasta || excede} onClick={aplicar}>
            Ver estas fechas
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
