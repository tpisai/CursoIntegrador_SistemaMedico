"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { rutasAdmin, responderSolicitud } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { ResponderSolicitudRequest, SolicitudAdmin } from "@/lib/api/types"
import { formatDiaHora } from "@/lib/format"

export type Respuesta = { solicitud: SolicitudAdmin; decision: ResponderSolicitudRequest["decision"]; abierto: boolean }

// Formulario RF-17 / RF-18: administración aprueba o rechaza lo que pidió el doctor.
export function ResponderSolicitudDialog({ respuesta, onCerrar }: { respuesta: Respuesta | null; onCerrar: () => void }) {
  const { mutate } = useSWRConfig()
  const [texto, setTexto] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [enviando, startTransition] = useTransition()

  const aprobar = respuesta?.decision === "APROBAR"
  const esCambio = respuesta?.solicitud.tipo === "CAMBIO_HORARIO"

  function cerrar() {
    setTexto("")
    setError(null)
    setErrorApi(null)
    onCerrar()
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!respuesta) return
    if (!aprobar && texto.trim().length < 5) {
      setError("Explica al doctor por qué se rechaza.")
      return
    }
    setError(null)
    setErrorApi(null)
    startTransition(async () => {
      try {
        await responderSolicitud(respuesta.solicitud.idSolicitud, {
          decision: respuesta.decision,
          respuesta: texto.trim() || undefined,
        })
        toast.success(
          aprobar
            ? `Solicitud aprobada. Avisamos a ${respuesta.solicitud.paciente} y al doctor.`
            : "Solicitud rechazada. Avisamos al doctor con tu motivo."
        )
        await Promise.all([
          mutate(rutasAdmin.solicitudes),
          mutate(rutasAdmin.resumen),
          mutate(RUTA_NOTIFICACIONES),
        ])
        cerrar()
      } catch (e) {
        setErrorApi(mensajeDeError(e))
      }
    })
  }

  return (
    <Dialog open={respuesta?.abierto ?? false} onOpenChange={(abrir) => !abrir && cerrar()}>
      {respuesta ? (
        <DialogContent className="gap-5 sm:max-w-[480px] sm:p-8">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold">
              {aprobar ? "Aprobar" : "Rechazar"} {esCambio ? "cambio de fecha" : "derivación"}
            </DialogTitle>
            <DialogDescription>
              {respuesta.solicitud.doctor} · {respuesta.solicitud.paciente} (DNI {respuesta.solicitud.dniPaciente}),
              cita del {formatDiaHora(respuesta.solicitud.fechaCita, respuesta.solicitud.horaCita)}.{" "}
              {respuesta.solicitud.detalle}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={onSubmit} noValidate>
            <FieldGroup className="gap-4">
              {aprobar ? (
                <Alert variant="brand">
                  <AlertDescription>
                    {esCambio
                      ? "La cita se moverá a la nueva fecha y el paciente recibirá una alerta automática."
                      : "El paciente recibirá un aviso para recoger su hoja de referencia en admisión."}
                  </AlertDescription>
                </Alert>
              ) : null}
              <Field data-invalid={!!error}>
                <FieldLabel htmlFor="respuesta-texto">
                  {aprobar ? "Comentario para el doctor (opcional)" : "Motivo del rechazo"}
                </FieldLabel>
                <Textarea
                  id="respuesta-texto"
                  maxLength={500}
                  placeholder={aprobar ? "Ej. Aprobado, se avisó al paciente." : "Ej. No hay turnos libres esa semana."}
                  aria-invalid={!!error}
                  value={texto}
                  onChange={(e) => {
                    setTexto(e.target.value)
                    setError(null)
                  }}
                />
                {error ? <FieldError>{error}</FieldError> : <FieldDescription>El doctor lo verá en sus notificaciones.</FieldDescription>}
              </Field>
              {errorApi ? (
                <Alert variant="destructive">
                  <CircleAlertIcon />
                  <AlertDescription>{errorApi}</AlertDescription>
                </Alert>
              ) : null}
            </FieldGroup>

            <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" size="lg" onClick={cerrar} disabled={enviando}>
                Volver
              </Button>
              <Button type="submit" size="lg" variant={aprobar ? "default" : "destructive"} disabled={enviando}>
                {enviando ? <Spinner data-icon="inline-start" /> : null}
                {aprobar ? "Aprobar y alertar al paciente" : "Rechazar solicitud"}
              </Button>
            </div>
          </form>
        </DialogContent>
      ) : null}
    </Dialog>
  )
}
