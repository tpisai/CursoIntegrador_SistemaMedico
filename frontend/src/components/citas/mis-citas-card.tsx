"use client"

import { useState } from "react"
import Link from "next/link"
import { CalendarPlusIcon, CalendarXIcon, CircleAlertIcon } from "lucide-react"
import { useSWRConfig } from "swr"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { type EstadoTicket, TicketCitaDialog } from "@/components/citas/ticket-cita-dialog"
import { useMisCitas } from "@/hooks/use-citas"
import { cancelarCita, confirmarAsistencia } from "@/lib/api/citas"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { Cita } from "@/lib/api/types"
import { formatDiaCorto, formatDiaHora } from "@/lib/format"

export function MisCitasCard({
  mostrarAgendar = false,
  conTicket = true,
}: {
  mostrarAgendar?: boolean
  // El inicio ya muestra la próxima cita arriba.
  conTicket?: boolean
}) {
  const { data: citas, error, isLoading, mutate: recargar } = useMisCitas()
  const { mutate } = useSWRConfig()
  const [ticket, setTicket] = useState<EstadoTicket | null>(null)
  const [porCancelar, setPorCancelar] = useState<Cita | null>(null)

  function cancelar(cita: Cita) {
    const promesa = cancelarCita(cita.idCita).then(() =>
      Promise.all([
        recargar(),
        // Libera el cupo en el calendario y en la lista de horarios.
        mutate((clave) => typeof clave === "string" && clave.startsWith("/horarios")),
        mutate(RUTA_NOTIFICACIONES),
      ])
    )
    toast.promise(promesa, {
      loading: "Cancelando tu cita…",
      success: `Cancelaste tu cita del ${formatDiaCorto(cita.fecha)}. El cupo quedó libre.`,
      error: mensajeDeError,
    })
  }

  function confirmar(cita: Cita) {
    toast.promise(confirmarAsistencia(cita.idCita).then(() => recargar()), {
      loading: "Confirmando tu asistencia…",
      success: `Confirmaste tu cita del ${formatDiaCorto(cita.fecha)}.`,
      error: mensajeDeError,
    })
  }

  const proxima = citas?.[0]

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Mis citas activas</CardTitle>
        {mostrarAgendar ? (
          <CardAction>
            <Button asChild size="sm">
              <Link href="/paciente/agendar">
                <CalendarPlusIcon data-icon="inline-start" />
                Agendar cita
              </Link>
            </Button>
          </CardAction>
        ) : null}
      </CardHeader>

      <CardContent className="flex flex-col gap-4">
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-9" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !citas?.length ? (
          <Empty className="border border-dashed p-6">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <CalendarXIcon />
              </EmptyMedia>
              <EmptyTitle>No tienes citas activas</EmptyTitle>
              <EmptyDescription>Cuando reserves una cita aparecerá aquí con su ticket de atención.</EmptyDescription>
            </EmptyHeader>
            {mostrarAgendar ? (
              <EmptyContent>
                <Button asChild>
                  <Link href="/paciente/agendar">Agendar cita</Link>
                </Button>
              </EmptyContent>
            ) : null}
          </Empty>
        ) : (
          <>
            <ul className="flex flex-col gap-3 md:hidden">
              {citas.map((cita) => (
                <li key={cita.idCita}>
                  <Card size="sm">
                    <CardContent className="flex flex-col gap-3">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex min-w-0 flex-col gap-0.5">
                          <span className="font-semibold">{formatDiaHora(cita.fecha, cita.horaInicio)}</span>
                          <span className="text-muted-foreground">
                            {cita.especialidad}, {cita.doctor}
                          </span>
                        </div>
                        <EstadoCitaBadge estado={cita.estado} />
                      </div>
                      {cita.estado === "RESERVADA" ? (
                        <Button className="h-11" onClick={() => confirmar(cita)}>
                          Confirmar asistencia
                          <span className="sr-only"> a la cita del {formatDiaCorto(cita.fecha)}</span>
                        </Button>
                      ) : null}
                      <div className="grid grid-cols-2 gap-2">
                        <Button
                          variant="outline"
                          className="h-11"
                          onClick={() => setTicket({ cita, recienReservada: false, abierto: true })}
                        >
                          Ver ticket
                          <span className="sr-only"> {cita.numeroTicket}</span>
                        </Button>
                        <Button variant="destructive" className="h-11" onClick={() => setPorCancelar(cita)}>
                          Cancelar
                          <span className="sr-only"> cita del {formatDiaCorto(cita.fecha)}</span>
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                </li>
              ))}
            </ul>

            <div className="hidden md:block">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fecha / hora</TableHead>
                    <TableHead>Especialidad</TableHead>
                    <TableHead>Doctor</TableHead>
                    <TableHead>Ticket</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead>
                      <span className="sr-only">Acciones</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {citas.map((cita) => (
                    <TableRow key={cita.idCita}>
                      <TableCell className="font-medium">{formatDiaHora(cita.fecha, cita.horaInicio)}</TableCell>
                      <TableCell>{cita.especialidad}</TableCell>
                      <TableCell>{cita.doctor}</TableCell>
                      <TableCell>
                        <Button
                          variant="link"
                          className="h-auto p-0 font-semibold"
                          onClick={() => setTicket({ cita, recienReservada: false, abierto: true })}
                        >
                          {cita.numeroTicket}
                          <span className="sr-only">: ver ticket</span>
                        </Button>
                      </TableCell>
                      <TableCell>
                        <EstadoCitaBadge estado={cita.estado} />
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          {cita.estado === "RESERVADA" ? (
                            <Button size="sm" onClick={() => confirmar(cita)}>
                              Confirmar asistencia
                              <span className="sr-only"> a la cita del {formatDiaCorto(cita.fecha)}</span>
                            </Button>
                          ) : null}
                          <Button variant="destructive" size="sm" onClick={() => setPorCancelar(cita)}>
                            Cancelar
                            <span className="sr-only"> cita del {formatDiaCorto(cita.fecha)}</span>
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {conTicket && proxima ? (
              <Alert
                variant="brand"
                role="note"
                className="flex flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:gap-5"
              >
                <div className="flex shrink-0 flex-col">
                  <span className="text-xs font-semibold">Ticket de atención</span>
                  <span className="text-xl font-bold text-primary">N.° {proxima.numeroTicket}</span>
                </div>
                <AlertDescription className="flex-1">
                  Presenta este número en recepción para validar tu cita del{" "}
                  {formatDiaHora(proxima.fecha, proxima.horaInicio).replace(" · ", ", ")} — {proxima.especialidad}.
                </AlertDescription>
                <Button
                  size="sm"
                  className="shrink-0 self-start sm:self-center"
                  onClick={() => setTicket({ cita: proxima, recienReservada: false, abierto: true })}
                >
                  Ver ticket
                </Button>
              </Alert>
            ) : null}
          </>
        )}
      </CardContent>

      <TicketCitaDialog ticket={ticket} onCerrar={() => setTicket((t) => t && { ...t, abierto: false })} />

      <AlertDialog open={porCancelar !== null} onOpenChange={(abierto) => !abierto && setPorCancelar(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Cancelar esta cita?</AlertDialogTitle>
            <AlertDialogDescription>
              {porCancelar
                ? `Se liberará tu cupo de ${porCancelar.especialidad} del ${formatDiaHora(porCancelar.fecha, porCancelar.horaInicio)} con ${porCancelar.doctor}. Esta acción no se puede deshacer.`
                : null}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Volver</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={() => porCancelar && cancelar(porCancelar)}>
              Sí, cancelar cita
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  )
}
