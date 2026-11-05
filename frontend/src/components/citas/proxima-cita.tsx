"use client"

import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { type EstadoTicket, TicketCitaDialog } from "@/components/citas/ticket-cita-dialog"
import { useMisCitas } from "@/hooks/use-citas"
import { formatDiaHora } from "@/lib/format"

// Lo primero que ve el paciente al entrar: cuándo y dónde es su próxima cita.
export function ProximaCita() {
  const { data: citas, isLoading } = useMisCitas()
  const [ticket, setTicket] = useState<EstadoTicket | null>(null)
  const proxima = citas?.[0]

  if (isLoading) return <Skeleton className="h-36 rounded-xl" />
  if (!proxima) return null

  return (
    <section
      aria-label="Tu próxima cita"
      className="flex flex-col gap-5 rounded-xl bg-brand-strong p-6 text-brand-foreground sm:flex-row sm:items-center sm:justify-between lg:px-8"
    >
      <div className="flex flex-col gap-1">
        <p className="text-sm font-medium opacity-85">Tu próxima cita</p>
        <p className="text-[28px] leading-9 font-semibold tracking-tight lg:text-[32px] lg:leading-10">
          {formatDiaHora(proxima.fecha, proxima.horaInicio)}
        </p>
        <p className="opacity-85">
          {proxima.especialidad} con {proxima.doctor}, {proxima.consultorio}
        </p>
      </div>
      <Button
        variant="secondary"
        size="xl"
        className="shrink-0"
        onClick={() => setTicket({ cita: proxima, recienReservada: false, abierto: true })}
      >
        Ver ticket {proxima.numeroTicket}
      </Button>
      <TicketCitaDialog ticket={ticket} onCerrar={() => setTicket((t) => t && { ...t, abierto: false })} />
    </section>
  )
}
