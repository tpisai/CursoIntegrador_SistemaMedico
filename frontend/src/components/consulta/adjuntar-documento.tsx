"use client"

import { Field, FieldDescription, FieldError, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { CampoSelect } from "@/components/portal/campo-select"
import { useTiposDocumento } from "@/hooks/use-doctor"

const MAX_ARCHIVO = 5 * 1024 * 1024
const TIPOS_ARCHIVO = ["application/pdf", "image/jpeg", "image/png"]

export type Adjunto = { tipoDocumento: string; archivo: File | null }
export type ErroresAdjunto = { tipoDocumento?: string; archivo?: string }

export function validarAdjunto({ tipoDocumento, archivo }: Adjunto): ErroresAdjunto {
  const errores: ErroresAdjunto = {}
  if (!archivo) return errores
  if (!tipoDocumento) errores.tipoDocumento = "Elige qué tipo de documento adjuntas."
  if (!TIPOS_ARCHIVO.includes(archivo.type)) errores.archivo = "Solo PDF, JPG o PNG."
  else if (archivo.size > MAX_ARCHIVO) errores.archivo = "El archivo supera 5 MB."
  return errores
}

/** Archivo opcional que acompaña a la atención; el paciente lo descarga desde su historial. */
export function AdjuntarDocumento({
  id,
  descripcion,
  adjunto,
  onCambio,
  errores,
}: {
  id: string
  descripcion: string
  adjunto: Adjunto
  onCambio: (cambios: Partial<Adjunto>) => void
  errores: ErroresAdjunto
}) {
  const tipos = useTiposDocumento()

  return (
    <FieldSet>
      <FieldLegend variant="label">Documento adjunto (opcional)</FieldLegend>
      <FieldDescription>{descripcion}</FieldDescription>
      <div className="grid gap-4 sm:grid-cols-2">
        <CampoSelect
          id={`${id}-tipo`}
          etiqueta="Tipo de documento"
          placeholder="Elige el tipo"
          opciones={tipos.data?.map((tipo) => ({ valor: tipo, texto: tipo }))}
          valor={adjunto.tipoDocumento}
          onCambio={(tipoDocumento) => onCambio({ tipoDocumento })}
          error={errores.tipoDocumento}
        />
        <Field data-invalid={!!errores.archivo}>
          <FieldLabel htmlFor={`${id}-archivo`}>Archivo (PDF, JPG o PNG, hasta 5 MB)</FieldLabel>
          <Input
            id={`${id}-archivo`}
            type="file"
            accept="application/pdf,image/jpeg,image/png"
            className="h-11"
            aria-invalid={!!errores.archivo}
            onChange={(e) => onCambio({ archivo: e.target.files?.[0] ?? null })}
          />
          <FieldError>{errores.archivo}</FieldError>
        </Field>
      </div>
    </FieldSet>
  )
}
