"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { CampoSelect } from "@/components/doctor/campo-select"
import { useCitasProximas, useHorariosLibres } from "@/hooks/use-doctor"
import { rutasDoctor, solicitarCambioFecha } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import { formatDiaHora } from "@/lib/format"

type Errores = Partial<Record<"cita" | "horario" | "motivo", string>>

// RF-15: el doctor propone una nueva fecha; administración aprueba y notifica al paciente.
export function CambioFechaCard() {
  const { mutate } = useSWRConfig()
  const citas = useCitasProximas()
  const horarios = useHorariosLibres()
  const [idCita, setIdCita] = useState("")
  const [idHorario, setIdHorario] = useState("")
  const [motivo, setMotivo] = useState("")
  const [errores, setErrores] = useState<Errores>({})
  const [enviando, startTransition] = useTransition()

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nuevos: Errores = {}
    if (!idCita) nuevos.cita = "Elige la cita que quieres cambiar."
    if (!idHorario) nuevos.horario = "Elige la nueva fecha propuesta."
    if (motivo.trim().length < 5) nuevos.motivo = "Explica brevemente el motivo del cambio."
    setErrores(nuevos)
    if (Object.keys(nuevos).length) return

    startTransition(async () => {
      try {
        await solicitarCambioFecha({ idCita: Number(idCita), idHorarioNuevo: Number(idHorario), motivo: motivo.trim() })
        toast.success("Solicitud enviada. Administración la revisará y avisará al paciente.")
        setIdCita("")
        setIdHorario("")
        setMotivo("")
        await Promise.all([mutate(rutasDoctor.solicitudes), mutate(RUTA_NOTIFICACIONES)])
      } catch (error) {
        toast.error(mensajeDeError(error))
      }
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Solicitar cambio de fecha</CardTitle>
        <CardDescription>
          La solicitud se envía a administración; si se aprueba, el paciente recibe una alerta automática.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            <CampoSelect
              id="cambio-cita"
              etiqueta="Cita activa"
              placeholder="Elige una cita"
              vacio="No tienes citas activas"
              opciones={citas.data?.map((c) => ({
                valor: String(c.idCita),
                texto: `${c.paciente} · ${formatDiaHora(c.fecha, c.horaInicio)}`,
              }))}
              valor={idCita}
              onCambio={setIdCita}
              error={errores.cita}
            />
            <CampoSelect
              id="cambio-horario"
              etiqueta="Nueva fecha propuesta"
              placeholder="Elige un turno libre"
              vacio="No tienes turnos libres próximos"
              opciones={horarios.data?.map((h) => ({
                valor: String(h.idHorario),
                texto: formatDiaHora(h.fecha, h.horaInicio),
              }))}
              valor={idHorario}
              onCambio={setIdHorario}
              error={errores.horario}
            />
            <Field data-invalid={!!errores.motivo}>
              <FieldLabel htmlFor="cambio-motivo">Motivo</FieldLabel>
              <Textarea
                id="cambio-motivo"
                placeholder="Ej. Reunión clínica programada por la jefatura…"
                value={motivo}
                maxLength={500}
                aria-invalid={!!errores.motivo}
                onChange={(e) => setMotivo(e.target.value)}
              />
              <FieldError>{errores.motivo}</FieldError>
            </Field>
            <Button type="submit" size="xl" className="w-full" disabled={enviando}>
              {enviando ? <Spinner data-icon="inline-start" /> : null}
              Enviar solicitud a administración
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
