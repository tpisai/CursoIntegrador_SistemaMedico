"use client"

import { useState, useTransition } from "react"
import { CalendarPlusIcon, CircleAlertIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { CampoSelect } from "@/components/portal/campo-select"
import { useConsultorios, useDoctores, useEspecialidades } from "@/hooks/use-citas"
import { crearHorarios } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import type { CrearHorariosResponse } from "@/lib/api/types"
import { formatDiaCorto, toFechaISO } from "@/lib/format"

const DURACIONES = [15, 20, 30, 45, 60].map((m) => ({ valor: String(m), texto: `${m} min` }))

type Datos = {
  idEspecialidad: string
  idDoctor: string
  idConsultorio: string
  fecha: string
  duracion: string
  horaInicio: string
  horaFin: string
}
type Errores = Partial<Record<keyof Datos, string>>

function minutos(hora: string) {
  const [h, m] = hora.split(":").map(Number)
  return h * 60 + m
}

// Formulario RF-13: admisión define los turnos que se podrán reservar en línea (tabla horario).
export function CrearHorariosCard() {
  const [hoy] = useState(() => toFechaISO(new Date()))
  const [datos, setDatos] = useState<Datos>(() => {
    const manana = new Date()
    manana.setDate(manana.getDate() + 1)
    return {
      idEspecialidad: "",
      idDoctor: "",
      idConsultorio: "",
      fecha: toFechaISO(manana),
      duracion: "30",
      horaInicio: "08:00",
      horaFin: "12:00",
    }
  })
  const [errores, setErrores] = useState<Errores>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [resultado, setResultado] = useState<CrearHorariosResponse | null>(null)
  const [creando, startTransition] = useTransition()

  const especialidades = useEspecialidades()
  const doctores = useDoctores(datos.idEspecialidad ? Number(datos.idEspecialidad) : undefined)
  const consultorios = useConsultorios(datos.idDoctor ? Number(datos.idDoctor) : undefined)

  function actualizar(cambios: Partial<Datos>) {
    setDatos((d) => ({ ...d, ...cambios }))
    // El aviso de un campo desaparece en cuanto se corrige.
    setErrores((e) => {
      const restantes = { ...e }
      for (const campo of Object.keys(cambios) as (keyof Datos)[]) delete restantes[campo]
      return restantes
    })
  }

  function validar(): Errores {
    const e: Errores = {}
    if (!datos.idEspecialidad) e.idEspecialidad = "Elige la especialidad."
    if (!datos.idDoctor) e.idDoctor = "Elige al doctor."
    if (!datos.idConsultorio) e.idConsultorio = "Elige el consultorio."
    if (!datos.fecha) e.fecha = "Elige la fecha."
    else if (datos.fecha < hoy) e.fecha = "La fecha no puede ser pasada."
    if (!datos.horaInicio) e.horaInicio = "Indica la hora de inicio."
    if (!datos.horaFin) e.horaFin = "Indica la hora de fin."
    else if (datos.horaInicio && minutos(datos.horaFin) - minutos(datos.horaInicio) < Number(datos.duracion)) {
      e.horaFin = "El rango debe alcanzar al menos para un turno."
    }
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
        const res = await crearHorarios({
          idDoctor: Number(datos.idDoctor),
          idConsultorio: Number(datos.idConsultorio),
          fecha: datos.fecha,
          horaInicio: datos.horaInicio,
          horaFin: datos.horaFin,
          duracionMinutos: Number(datos.duracion),
        })
        setResultado(res)
        if (res.creados > 0) toast.success(`Se crearon ${res.creados} turnos para ${res.doctor}.`)
        else toast.warning("No se creó ningún turno: ese doctor ya tenía horarios en ese rango.")
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Gestionar nuevos horarios de citas</CardTitle>
        <CardDescription>
          Define los turnos por doctor y especialidad que estarán disponibles para reserva en línea.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            <CampoSelect
              id="horario-especialidad"
              etiqueta="Especialidad"
              placeholder="Elige una especialidad"
              opciones={especialidades.data?.map((e) => ({ valor: String(e.idEspecialidad), texto: e.nombre }))}
              valor={datos.idEspecialidad}
              onCambio={(v) => actualizar({ idEspecialidad: v, idDoctor: "", idConsultorio: "" })}
              error={errores.idEspecialidad}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <CampoSelect
                id="horario-doctor"
                etiqueta="Doctor"
                placeholder="Elige un doctor"
                vacio={datos.idEspecialidad ? "Sin doctores en esta especialidad" : "Primero elige la especialidad"}
                opciones={datos.idEspecialidad ? doctores.data?.map((d) => ({ valor: String(d.idDoctor), texto: d.nombreCompleto })) : []}
                valor={datos.idDoctor}
                onCambio={(v) => actualizar({ idDoctor: v, idConsultorio: "" })}
                error={errores.idDoctor}
              />
              <CampoSelect
                id="horario-consultorio"
                etiqueta="Consultorio"
                placeholder="Elige un consultorio"
                vacio={datos.idDoctor ? "El doctor no tiene consultorio" : "Primero elige al doctor"}
                opciones={
                  datos.idDoctor
                    ? consultorios.data?.map((c) => ({
                        valor: String(c.idConsultorio),
                        texto: c.zona ? `${c.zona} · ${c.nombre}` : c.nombre,
                      }))
                    : []
                }
                valor={datos.idConsultorio}
                onCambio={(v) => actualizar({ idConsultorio: v })}
                error={errores.idConsultorio}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.fecha}>
                <FieldLabel htmlFor="horario-fecha">Fecha</FieldLabel>
                <Input
                  id="horario-fecha"
                  type="date"
                  min={hoy}
                  className="h-10"
                  aria-invalid={!!errores.fecha}
                  value={datos.fecha}
                  onChange={(e) => actualizar({ fecha: e.target.value })}
                />
                <FieldError>{errores.fecha}</FieldError>
              </Field>
              <CampoSelect
                id="horario-duracion"
                etiqueta="Duración de cada turno"
                placeholder="Elige la duración"
                opciones={DURACIONES}
                valor={datos.duracion}
                onCambio={(v) => actualizar({ duracion: v })}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.horaInicio}>
                <FieldLabel htmlFor="horario-inicio">Hora de inicio</FieldLabel>
                <Input
                  id="horario-inicio"
                  type="time"
                  step={300}
                  className="h-10"
                  aria-invalid={!!errores.horaInicio}
                  value={datos.horaInicio}
                  onChange={(e) => actualizar({ horaInicio: e.target.value })}
                />
                <FieldError>{errores.horaInicio}</FieldError>
              </Field>
              <Field data-invalid={!!errores.horaFin}>
                <FieldLabel htmlFor="horario-fin">Hora de fin</FieldLabel>
                <Input
                  id="horario-fin"
                  type="time"
                  step={300}
                  className="h-10"
                  aria-invalid={!!errores.horaFin}
                  value={datos.horaFin}
                  onChange={(e) => actualizar({ horaFin: e.target.value })}
                />
                <FieldError>{errores.horaFin}</FieldError>
              </Field>
            </div>

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}
            {resultado ? (
              <Alert variant={resultado.creados > 0 ? "brand" : "warning"} role="status">
                <CalendarPlusIcon />
                <AlertTitle>
                  {resultado.creados > 0 ? `${resultado.creados} turnos creados` : "No se crearon turnos"}
                </AlertTitle>
                <AlertDescription>
                  {resultado.doctor} · {formatDiaCorto(resultado.fecha)} · {resultado.consultorio}.
                  {resultado.omitidos > 0 ? ` ${resultado.omitidos} se omitieron porque ya había horarios en esas horas.` : ""}
                </AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="w-full" disabled={creando}>
              {creando ? <Spinner data-icon="inline-start" /> : null}
              Crear horarios
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
