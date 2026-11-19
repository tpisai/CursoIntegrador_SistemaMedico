"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { CampoSelect } from "@/components/portal/campo-select"
import { useCitasProximas } from "@/hooks/use-doctor"
import { rutasDoctor, solicitarCambioFecha } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import { formatDiaHora } from "@/lib/format"

type Errores = Partial<Record<"cita" | "motivo", string>>

// RF-15/RF-17: el doctor solo justifica el cambio; administración lo aprueba y el paciente elige el nuevo horario.
export function CambioFechaCard() {
  const { mutate } = useSWRConfig()
  const citas = useCitasProximas()
  const [idCita, setIdCita] = useState("")
  const [motivo, setMotivo] = useState("")
  const [errores, setErrores] = useState<Errores>({})
  const [enviando, startTransition] = useTransition()

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nuevos: Errores = {}
    if (!idCita) nuevos.cita = "Elige la cita que quieres cambiar."
    if (motivo.trim().length < 5) nuevos.motivo = "Explica brevemente el motivo del cambio."
    setErrores(nuevos)
    if (Object.keys(nuevos).length) return

    startTransition(async () => {
      try {
        await solicitarCambioFecha({ idCita: Number(idCita), motivo: motivo.trim() })
        toast.success("Solicitud enviada. Administración la revisará y avisará al paciente.")
        setIdCita("")
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
          La solicitud se envía a administración; si se aprueba, el paciente elige el nuevo horario antes de
          continuar usando su portal.
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
