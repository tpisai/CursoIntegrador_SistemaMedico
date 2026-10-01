"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { Consultorio, Doctor, Especialidad } from "@/lib/api/types"

type Opcion = { valor: number; texto: string }

function Filtro({
  id,
  etiqueta,
  opciones,
  valor,
  onCambio,
}: {
  id: string
  etiqueta: string
  opciones: Opcion[] | undefined
  valor: number | undefined
  onCambio: (valor: number) => void
}) {
  return (
    <Field>
      <FieldLabel htmlFor={id}>{etiqueta}</FieldLabel>
      <Select
        value={valor ? String(valor) : ""}
        onValueChange={(v) => onCambio(Number(v))}
        disabled={!opciones?.length}
      >
        <SelectTrigger id={id} size="lg" className="w-full">
          <SelectValue placeholder={opciones ? "Sin opciones" : "Cargando…"} />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {opciones?.map((opcion) => (
              <SelectItem key={opcion.valor} value={String(opcion.valor)}>
                {opcion.texto}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </Field>
  )
}

export function FiltrosDisponibilidad({
  especialidades,
  doctores,
  consultorios,
  idEspecialidad,
  idDoctor,
  idConsultorio,
  onEspecialidad,
  onDoctor,
  onConsultorio,
}: {
  especialidades: Especialidad[] | undefined
  doctores: Doctor[] | undefined
  consultorios: Consultorio[] | undefined
  idEspecialidad: number | undefined
  idDoctor: number | undefined
  idConsultorio: number | undefined
  onEspecialidad: (id: number) => void
  onDoctor: (id: number) => void
  onConsultorio: (id: number) => void
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Buscar disponibilidad</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col justify-between gap-6">
        <FieldGroup className="gap-4">
          <Filtro
            id="filtro-especialidad"
            etiqueta="Especialidad"
            opciones={especialidades?.map((e) => ({ valor: e.idEspecialidad, texto: e.nombre }))}
            valor={idEspecialidad}
            onCambio={onEspecialidad}
          />
          <Filtro
            id="filtro-doctor"
            etiqueta="Doctor"
            opciones={doctores?.map((d) => ({ valor: d.idDoctor, texto: d.nombreCompleto }))}
            valor={idDoctor}
            onCambio={onDoctor}
          />
          <Filtro
            id="filtro-consultorio"
            etiqueta="Consultorio / zona"
            opciones={consultorios?.map((c) => ({
              valor: c.idConsultorio,
              texto: c.zona ? `${c.zona} · ${c.nombre}` : c.nombre,
            }))}
            valor={idConsultorio}
            onCambio={onConsultorio}
          />
        </FieldGroup>

        <p className="flex items-center gap-2.5 text-xs text-muted-foreground">
          <span className="relative flex size-2 shrink-0" aria-hidden="true">
            <span className="absolute inline-flex size-full animate-ping rounded-full bg-primary opacity-60 motion-reduce:hidden" />
            <span className="relative inline-flex size-2 rounded-full bg-primary" />
          </span>
          Disponibilidad actualizada en tiempo real
        </p>
      </CardContent>
    </Card>
  )
}
