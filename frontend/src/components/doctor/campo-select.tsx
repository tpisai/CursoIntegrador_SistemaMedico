"use client"

import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export type OpcionSelect = { valor: string; texto: string }

/** Select con etiqueta y mensaje de error, para los formularios del panel del doctor. */
export function CampoSelect({
  id,
  etiqueta,
  opciones,
  valor,
  onCambio,
  error,
  placeholder,
  vacio = "Sin opciones disponibles",
}: {
  id: string
  etiqueta: string
  opciones: OpcionSelect[] | undefined
  valor: string
  onCambio: (valor: string) => void
  error?: string
  placeholder: string
  vacio?: string
}) {
  const sinOpciones = opciones !== undefined && opciones.length === 0
  return (
    <Field data-invalid={!!error}>
      <FieldLabel htmlFor={id}>{etiqueta}</FieldLabel>
      <Select value={valor} onValueChange={onCambio} disabled={!opciones?.length}>
        <SelectTrigger id={id} size="lg" className="w-full" aria-invalid={!!error}>
          <SelectValue placeholder={opciones ? (sinOpciones ? vacio : placeholder) : "Cargando…"} />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {opciones?.map((opcion) => (
              <SelectItem key={opcion.valor} value={opcion.valor}>
                {opcion.texto}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <FieldError>{error}</FieldError>
    </Field>
  )
}
