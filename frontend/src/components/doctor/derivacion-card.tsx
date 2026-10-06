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
import { ESPECIALIDADES_DERIVACION, ESTABLECIMIENTOS } from "@/components/doctor/opciones-derivacion"
import { useCitasProximas } from "@/hooks/use-doctor"
import { rutasDoctor, solicitarDerivacion } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import { formatDiaCorto } from "@/lib/format"

type Errores = Partial<Record<"cita" | "establecimiento" | "especialidad" | "motivo", string>>

const aOpciones = (lista: string[]) => lista.map((texto) => ({ valor: texto, texto }))

// RF-16: orden de derivación; administración procesa el traslado y notifica al paciente (RF-18).
export function DerivacionCard() {
  const { mutate } = useSWRConfig()
  const citas = useCitasProximas()
  const [idCita, setIdCita] = useState("")
  const [establecimiento, setEstablecimiento] = useState("")
  const [especialidad, setEspecialidad] = useState("")
  const [motivo, setMotivo] = useState("")
  const [errores, setErrores] = useState<Errores>({})
  const [enviando, startTransition] = useTransition()

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nuevos: Errores = {}
    if (!idCita) nuevos.cita = "Elige al paciente."
    if (!establecimiento) nuevos.establecimiento = "Elige el establecimiento de destino."
    if (!especialidad) nuevos.especialidad = "Elige la especialidad requerida."
    if (motivo.trim().length < 5) nuevos.motivo = "Explica brevemente el motivo de la derivación."
    setErrores(nuevos)
    if (Object.keys(nuevos).length) return

    startTransition(async () => {
      try {
        await solicitarDerivacion({ idCita: Number(idCita), establecimiento, especialidad, motivo: motivo.trim() })
        toast.success("Derivación enviada a administración.")
        setIdCita("")
        setEstablecimiento("")
        setEspecialidad("")
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
        <CardTitle className="font-semibold">Derivar paciente a otro hospital</CardTitle>
        <CardDescription>
          Genera la orden de derivación; administración procesa el traslado y notifica al paciente.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            <CampoSelect
              id="derivar-paciente"
              etiqueta="Paciente"
              placeholder="Elige un paciente"
              vacio="No tienes pacientes con citas activas"
              opciones={citas.data?.map((c) => ({
                valor: String(c.idCita),
                texto: `${c.paciente} · DNI ${c.dniPaciente} · ${formatDiaCorto(c.fecha)}`,
              }))}
              valor={idCita}
              onCambio={setIdCita}
              error={errores.cita}
            />
            <CampoSelect
              id="derivar-destino"
              etiqueta="Establecimiento destino"
              placeholder="Elige un establecimiento"
              opciones={aOpciones(ESTABLECIMIENTOS)}
              valor={establecimiento}
              onCambio={setEstablecimiento}
              error={errores.establecimiento}
            />
            <CampoSelect
              id="derivar-especialidad"
              etiqueta="Especialidad requerida"
              placeholder="Elige una especialidad"
              opciones={aOpciones(ESPECIALIDADES_DERIVACION)}
              valor={especialidad}
              onCambio={setEspecialidad}
              error={errores.especialidad}
            />
            <Field data-invalid={!!errores.motivo}>
              <FieldLabel htmlFor="derivar-motivo">Motivo de derivación</FieldLabel>
              <Textarea
                id="derivar-motivo"
                placeholder="Ej. Requiere evaluación especializada no disponible en el centro…"
                value={motivo}
                maxLength={500}
                aria-invalid={!!errores.motivo}
                onChange={(e) => setMotivo(e.target.value)}
              />
              <FieldError>{errores.motivo}</FieldError>
            </Field>
            <Button type="submit" size="xl" variant="soft" className="w-full" disabled={enviando}>
              {enviando ? <Spinner data-icon="inline-start" /> : null}
              Derivar a otro hospital
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
