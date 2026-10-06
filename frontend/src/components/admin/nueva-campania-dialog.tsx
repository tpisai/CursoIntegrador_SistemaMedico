"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { crearCampania, rutasAdmin } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import { toFechaISO } from "@/lib/format"

type Datos = {
  titulo: string
  descripcion: string
  fechaInicio: string
  fechaFin: string
  hora: string
  lugar: string
  cupos: string
}
type Errores = Partial<Record<keyof Datos, string>>

const VACIO: Datos = { titulo: "", descripcion: "", fechaInicio: "", fechaFin: "", hora: "", lugar: "", cupos: "" }

// Formulario RF-20: publica una campaña (tabla campania) y avisa a los pacientes (RF-19).
export function NuevaCampaniaDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { mutate } = useSWRConfig()
  const [hoy] = useState(() => toFechaISO(new Date()))
  const [datos, setDatos] = useState<Datos>(VACIO)
  const [errores, setErrores] = useState<Errores>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [publicando, startTransition] = useTransition()

  function actualizar(cambios: Partial<Datos>) {
    setDatos((d) => ({ ...d, ...cambios }))
    // El aviso de un campo desaparece en cuanto se corrige.
    setErrores((e) => {
      const restantes = { ...e }
      for (const campo of Object.keys(cambios) as (keyof Datos)[]) delete restantes[campo]
      return restantes
    })
  }

  function cambiarApertura(abierto: boolean) {
    if (!abierto) {
      setErrores({})
      setErrorApi(null)
    }
    onOpenChange(abierto)
  }

  function validar(): Errores {
    const e: Errores = {}
    if (datos.titulo.trim().length < 5) e.titulo = "Escribe un título de al menos 5 caracteres."
    if (!datos.fechaInicio) e.fechaInicio = "Indica la fecha de inicio."
    if (!datos.fechaFin) e.fechaFin = "Indica la fecha de fin."
    else if (datos.fechaFin < hoy) e.fechaFin = "La campaña no puede terminar en el pasado."
    else if (datos.fechaInicio && datos.fechaFin < datos.fechaInicio) e.fechaFin = "Debe ser igual o posterior al inicio."
    const cupos = Number(datos.cupos)
    if (!Number.isInteger(cupos) || cupos < 1) e.cupos = "Indica al menos 1 cupo."
    else if (cupos > 10000) e.cupos = "El máximo es 10 000 cupos."
    return e
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nuevos = validar()
    setErrores(nuevos)
    if (Object.keys(nuevos).length) return
    setErrorApi(null)
    startTransition(async () => {
      try {
        const campania = await crearCampania({
          titulo: datos.titulo.trim(),
          descripcion: datos.descripcion.trim() || undefined,
          fechaInicio: datos.fechaInicio,
          fechaFin: datos.fechaFin,
          hora: datos.hora || undefined,
          lugar: datos.lugar.trim() || undefined,
          cupos: Number(datos.cupos),
        })
        toast.success(`"${campania.titulo}" publicada. Avisamos a los pacientes en sus notificaciones.`)
        await Promise.all([mutate(rutasAdmin.campanias), mutate(rutasAdmin.resumen)])
        setDatos(VACIO)
        cambiarApertura(false)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent className="max-h-[calc(100svh-2rem)] gap-5 overflow-y-auto sm:max-w-[520px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Nueva campaña médica</DialogTitle>
          <DialogDescription>
            Los pacientes la verán en Comunicación y recibirán una notificación para inscribirse.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            <Field data-invalid={!!errores.titulo}>
              <FieldLabel htmlFor="campania-titulo">Título</FieldLabel>
              <Input
                id="campania-titulo"
                placeholder="Ej. Vacunación contra la influenza"
                maxLength={150}
                className="h-10"
                aria-invalid={!!errores.titulo}
                value={datos.titulo}
                onChange={(e) => actualizar({ titulo: e.target.value })}
              />
              <FieldError>{errores.titulo}</FieldError>
            </Field>
            <Field>
              <FieldLabel htmlFor="campania-descripcion">Descripción</FieldLabel>
              <Textarea
                id="campania-descripcion"
                placeholder="A quién va dirigida, qué incluye y qué deben traer."
                maxLength={2000}
                value={datos.descripcion}
                onChange={(e) => actualizar({ descripcion: e.target.value })}
              />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.fechaInicio}>
                <FieldLabel htmlFor="campania-inicio">Fecha de inicio</FieldLabel>
                <Input
                  id="campania-inicio"
                  type="date"
                  min={hoy}
                  className="h-10"
                  aria-invalid={!!errores.fechaInicio}
                  value={datos.fechaInicio}
                  onChange={(e) =>
                    actualizar({ fechaInicio: e.target.value, fechaFin: datos.fechaFin || e.target.value })
                  }
                />
                <FieldError>{errores.fechaInicio}</FieldError>
              </Field>
              <Field data-invalid={!!errores.fechaFin}>
                <FieldLabel htmlFor="campania-fin">Fecha de fin</FieldLabel>
                <Input
                  id="campania-fin"
                  type="date"
                  min={datos.fechaInicio || hoy}
                  className="h-10"
                  aria-invalid={!!errores.fechaFin}
                  value={datos.fechaFin}
                  onChange={(e) => actualizar({ fechaFin: e.target.value })}
                />
                <FieldError>{errores.fechaFin}</FieldError>
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_120px]">
              <Field>
                <FieldLabel htmlFor="campania-lugar">Lugar</FieldLabel>
                <Input
                  id="campania-lugar"
                  placeholder="Ej. Patio principal del centro de salud"
                  maxLength={255}
                  className="h-10"
                  value={datos.lugar}
                  onChange={(e) => actualizar({ lugar: e.target.value })}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="campania-hora">Hora</FieldLabel>
                <Input
                  id="campania-hora"
                  type="time"
                  step={300}
                  className="h-10"
                  value={datos.hora}
                  onChange={(e) => actualizar({ hora: e.target.value })}
                />
              </Field>
            </div>
            <Field data-invalid={!!errores.cupos}>
              <FieldLabel htmlFor="campania-cupos">Cupos</FieldLabel>
              <Input
                id="campania-cupos"
                type="number"
                inputMode="numeric"
                min={1}
                max={10000}
                placeholder="Ej. 100"
                className="h-10 sm:max-w-40"
                aria-invalid={!!errores.cupos}
                value={datos.cupos}
                onChange={(e) => actualizar({ cupos: e.target.value })}
              />
              {errores.cupos ? (
                <FieldError>{errores.cupos}</FieldError>
              ) : (
                <FieldDescription>Cada inscripción descuenta un cupo.</FieldDescription>
              )}
            </Field>

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="w-full" disabled={publicando}>
              {publicando ? <Spinner data-icon="inline-start" /> : null}
              Publicar campaña
            </Button>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
