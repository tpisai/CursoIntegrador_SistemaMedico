"use client"

import { useState } from "react"
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { CitasAsignadasCard } from "@/components/doctor/citas-asignadas-card"
import { formatFechaLarga, parseFecha, toFechaISO } from "@/lib/format"

function moverDias(fecha: string, dias: number) {
  const d = parseFecha(fecha)
  d.setDate(d.getDate() + dias)
  return toFechaISO(d)
}

/** Citas de hoy (panel) o de cualquier día con navegación (Mi agenda). */
export function AgendaDelDia({ navegable = false }: { navegable?: boolean }) {
  const [hoy] = useState(() => toFechaISO(new Date()))
  const [fecha, setFecha] = useState(hoy)
  const esHoy = fecha === hoy

  return (
    <CitasAsignadasCard
      fecha={fecha}
      descripcion={`Citas ya asignadas a tu agenda para ${esHoy ? "hoy, " : ""}${formatFechaLarga(fecha).toLowerCase()}.`}
      accion={
        navegable ? (
          <div className="flex items-center gap-1">
            <Button variant="outline" size="icon" onClick={() => setFecha((f) => moverDias(f, -1))}>
              <ChevronLeftIcon />
              <span className="sr-only">Día anterior</span>
            </Button>
            <Button variant="outline" disabled={esHoy} onClick={() => setFecha(hoy)}>
              Hoy
            </Button>
            <Button variant="outline" size="icon" onClick={() => setFecha((f) => moverDias(f, 1))}>
              <ChevronRightIcon />
              <span className="sr-only">Día siguiente</span>
            </Button>
          </div>
        ) : undefined
      }
    />
  )
}
