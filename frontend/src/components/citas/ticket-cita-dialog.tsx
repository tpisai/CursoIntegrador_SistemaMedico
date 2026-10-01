"use client"

import { useTransition } from "react"
import { CheckIcon, DownloadIcon } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Spinner } from "@/components/ui/spinner"
import { descargarTicket } from "@/lib/api/citas"
import { mensajeDeError } from "@/lib/api/http"
import type { Cita } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { formatDiaHora } from "@/lib/format"

export type EstadoTicket = { cita: Cita; recienReservada: boolean; abierto: boolean }

// Modal M3 · Cita confirmada del prototipo. También sirve para "Ver ticket" desde Mis citas.
export function TicketCitaDialog({
  ticket,
  onCerrar,
}: {
  ticket: EstadoTicket | null
  onCerrar: () => void
}) {
  const [descargando, startTransition] = useTransition()

  function descargar(cita: Cita) {
    startTransition(async () => {
      try {
        guardarArchivo(await descargarTicket(cita.idCita), `ticket-${cita.numeroTicket}.pdf`)
      } catch (error) {
        toast.error(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={ticket?.abierto ?? false} onOpenChange={(abierto) => !abierto && onCerrar()}>
      {ticket ? (
        <DialogContent className="gap-5 text-center sm:max-w-[440px] sm:p-8">
          {ticket.recienReservada ? (
            <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-success text-success-foreground">
              <CheckIcon className="size-7" strokeWidth={3} aria-hidden="true" />
            </div>
          ) : null}

          <DialogHeader className="items-center text-center">
            <DialogTitle className="text-xl font-bold">
              {ticket.recienReservada ? "¡Cita confirmada!" : "Ticket de atención"}
            </DialogTitle>
            <DialogDescription>
              {ticket.recienReservada
                ? "Tu cupo quedó bloqueado en tiempo real."
                : "Presenta este número en recepción para validar tu cita."}
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col items-center gap-1 rounded-xl bg-secondary px-6 py-4">
            <span className="text-[11px] font-semibold tracking-wider text-secondary-foreground uppercase">
              Ticket de atención
            </span>
            <span className="text-3xl font-bold tracking-tight text-primary">N.° {ticket.cita.numeroTicket}</span>
          </div>

          <div className="flex flex-col gap-1 text-sm">
            <p className="font-medium">
              {ticket.cita.especialidad} · {ticket.cita.doctor}
            </p>
            <p className="text-muted-foreground">
              {formatDiaHora(ticket.cita.fecha, ticket.cita.horaInicio)} · {ticket.cita.consultorio}
            </p>
          </div>

          <p className="text-xs leading-relaxed text-muted-foreground">
            Recibirás recordatorios por SMS, WhatsApp o correo 48 y 24 horas antes de tu cita.
          </p>

          <div className="flex flex-col gap-2">
            <DialogClose asChild>
              <Button size="xl" className="w-full">
                Entendido
              </Button>
            </DialogClose>
            <Button variant="link" disabled={descargando} onClick={() => descargar(ticket.cita)}>
              {descargando ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
              Descargar ticket (PDF)
            </Button>
          </div>
        </DialogContent>
      ) : null}
    </Dialog>
  )
}
