"use client"

import { useState, useTransition } from "react"
import { CircleAlertIcon, CircleCheckIcon, SearchIcon, TicketCheckIcon, TriangleAlertIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { consultarTicket, validarTicket } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import type { ResultadoTicket, TicketRecepcion } from "@/lib/api/types"
import { formatDiaHora } from "@/lib/format"

const AVISOS: Record<ResultadoTicket, { titulo: string; variant: "brand" | "warning" | "destructive" }> = {
  VALIDO: { titulo: "Ticket válido", variant: "brand" },
  YA_VALIDADO: { titulo: "Llegada registrada", variant: "brand" },
  OTRO_DIA: { titulo: "No le corresponde el turno", variant: "warning" },
  NO_ACTIVA: { titulo: "Cita no vigente", variant: "destructive" },
}

// Módulo de recepción (RF-12): se busca el número que presenta el paciente y se registra su llegada.
export function ValidarTicketCard() {
  const [numero, setNumero] = useState("")
  const [errorCampo, setErrorCampo] = useState<string | null>(null)
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [ticket, setTicket] = useState<TicketRecepcion | null>(null)
  const [buscando, startBuscar] = useTransition()
  const [validando, startValidar] = useTransition()

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const valor = numero.trim()
    if (!valor) {
      setErrorCampo("Escribe el número de ticket, por ejemplo A-0154.")
      return
    }
    setErrorApi(null)
    setTicket(null)
    startBuscar(async () => {
      try {
        setTicket(await consultarTicket(valor))
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  function validar() {
    if (!ticket) return
    startValidar(async () => {
      try {
        const validado = await validarTicket(ticket.numeroTicket)
        setTicket(validado)
        toast.success(`Llegada de ${validado.paciente} registrada. Se avisó a ${validado.doctor}.`)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  const aviso = ticket ? AVISOS[ticket.resultado] : null

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Validar ticket de atención</CardTitle>
        <CardDescription>
          Ingresa el número que presenta el paciente al llegar para confirmar su turno.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <FieldGroup className="gap-4">
          <form onSubmit={onSubmit} noValidate className="flex items-start gap-2">
            <Field data-invalid={!!errorCampo} className="flex-1">
              <FieldLabel htmlFor="ticket-numero" className="sr-only">
                Número de ticket
              </FieldLabel>
              <Input
                id="ticket-numero"
                placeholder="A-0154"
                autoComplete="off"
                className="h-10 font-mono uppercase"
                aria-invalid={!!errorCampo}
                value={numero}
                onChange={(e) => {
                  setNumero(e.target.value)
                  setErrorCampo(null)
                }}
              />
              <FieldError>{errorCampo}</FieldError>
            </Field>
            <Button type="submit" size="lg" className="h-10" disabled={buscando}>
              {buscando ? <Spinner data-icon="inline-start" /> : <SearchIcon data-icon="inline-start" />}
              Buscar
            </Button>
          </form>

          {errorApi ? (
            <Alert variant="destructive">
              <CircleAlertIcon />
              <AlertDescription>{errorApi}</AlertDescription>
            </Alert>
          ) : null}

          {ticket && aviso ? (
            <>
              <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1.5 rounded-lg border p-4 text-sm">
                <dt className="text-muted-foreground">Ticket</dt>
                <dd className="flex items-center gap-2 font-mono font-semibold">
                  {ticket.numeroTicket}
                  <EstadoCitaBadge estado={ticket.estado} />
                </dd>
                <dt className="text-muted-foreground">Paciente</dt>
                <dd>
                  {ticket.paciente} · DNI {ticket.dniPaciente}
                </dd>
                <dt className="text-muted-foreground">Cita</dt>
                <dd>
                  {ticket.especialidad} · {ticket.doctor}
                </dd>
                <dt className="text-muted-foreground">Fecha</dt>
                <dd>{formatDiaHora(ticket.fecha, ticket.horaInicio)}</dd>
                <dt className="text-muted-foreground">Lugar</dt>
                <dd>{ticket.consultorio}</dd>
              </dl>

              <Alert variant={aviso.variant} role="status">
                {ticket.resultado === "VALIDO" || ticket.resultado === "YA_VALIDADO" ? (
                  <CircleCheckIcon />
                ) : (
                  <TriangleAlertIcon />
                )}
                <AlertTitle>{aviso.titulo}</AlertTitle>
                <AlertDescription>{ticket.mensaje}</AlertDescription>
              </Alert>

              {ticket.resultado === "VALIDO" ? (
                <Button size="xl" className="w-full" onClick={validar} disabled={validando}>
                  {validando ? <Spinner data-icon="inline-start" /> : <TicketCheckIcon data-icon="inline-start" />}
                  Registrar llegada
                </Button>
              ) : null}
            </>
          ) : null}
        </FieldGroup>
      </CardContent>
    </Card>
  )
}
