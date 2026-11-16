"use client"

import { PlusIcon, Trash2Icon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldError, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import type { MedicamentoRequest } from "@/lib/api/types"

type Campo = Exclude<keyof MedicamentoRequest, "orden">

const CAMPOS: { campo: Campo; etiqueta: string; ejemplo: string; ancho?: string }[] = [
  { campo: "nombreMedicamento", etiqueta: "Medicamento", ejemplo: "Ej. Paracetamol", ancho: "sm:col-span-2" },
  { campo: "dosis", etiqueta: "Dosis", ejemplo: "Ej. 500 mg" },
  { campo: "presentacion", etiqueta: "Presentación", ejemplo: "Ej. Tableta" },
  { campo: "viaAdministracion", etiqueta: "Vía de administración", ejemplo: "Ej. Oral" },
  { campo: "unidadesPorToma", etiqueta: "Unidades por toma", ejemplo: "Ej. 1 tableta" },
  { campo: "frecuencia", etiqueta: "Frecuencia", ejemplo: "Ej. Cada 8 horas" },
  { campo: "duracion", etiqueta: "Duración", ejemplo: "Ej. 3 días" },
  { campo: "cantidad", etiqueta: "Cantidad total", ejemplo: "Ej. 9 tabletas", ancho: "sm:col-span-2" },
]

const VACIO: MedicamentoRequest = {
  nombreMedicamento: "",
  dosis: "",
  cantidad: "",
  viaAdministracion: "",
  presentacion: "",
  unidadesPorToma: "",
  frecuencia: "",
  duracion: "",
}

const tieneDatos = (m: MedicamentoRequest) => CAMPOS.some(({ campo }) => (m[campo] ?? "").trim() !== "")

/** Medicamentos listos para enviar: sin filas vacías y numerados en el orden en que se escribieron. */
export function medicamentosParaEnviar(medicamentos: MedicamentoRequest[]): MedicamentoRequest[] {
  return medicamentos.filter(tieneDatos).map((m, i) => ({ ...m, nombreMedicamento: m.nombreMedicamento.trim(), orden: i + 1 }))
}

/** Índices de las filas que tienen datos pero no el nombre del medicamento. */
export function medicamentosSinNombre(medicamentos: MedicamentoRequest[]): number[] {
  return medicamentos.flatMap((m, i) => (tieneDatos(m) && !m.nombreMedicamento.trim() ? [i] : []))
}

// Receta médica: con estos datos se arma el PDF que descargan el doctor y el paciente.
export function RecetaMedicamentos({
  medicamentos,
  onCambio,
  sinNombre,
}: {
  medicamentos: MedicamentoRequest[]
  onCambio: (medicamentos: MedicamentoRequest[]) => void
  sinNombre: number[]
}) {
  function actualizar(indice: number, campo: Campo, valor: string) {
    onCambio(medicamentos.map((m, i) => (i === indice ? { ...m, [campo]: valor } : m)))
  }

  return (
    <FieldSet>
      <FieldLegend variant="label">Receta médica</FieldLegend>
      <FieldDescription>
        Agrega los medicamentos indicados. Al guardar podrás descargar la receta en PDF.
      </FieldDescription>
      <div className="flex flex-col gap-3">
        {medicamentos.map((medicamento, indice) => (
          <div key={indice} className="flex flex-col gap-3 rounded-lg border p-4">
            <div className="flex items-center justify-between gap-3">
              <span className="font-medium">Medicamento {indice + 1}</span>
              <Button
                type="button"
                variant="ghost"
                size="icon-lg"
                onClick={() => onCambio(medicamentos.filter((_, i) => i !== indice))}
              >
                <Trash2Icon />
                <span className="sr-only">Quitar el medicamento {indice + 1}</span>
              </Button>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {CAMPOS.map(({ campo, etiqueta, ejemplo, ancho }) => {
                const id = `receta-${indice}-${campo}`
                const falta = campo === "nombreMedicamento" && sinNombre.includes(indice)
                return (
                  <Field key={campo} className={ancho} data-invalid={falta}>
                    <FieldLabel htmlFor={id}>{etiqueta}</FieldLabel>
                    <Input
                      id={id}
                      className="h-11"
                      placeholder={ejemplo}
                      maxLength={campo === "nombreMedicamento" ? 200 : 50}
                      aria-invalid={falta}
                      value={medicamento[campo] ?? ""}
                      onChange={(e) => actualizar(indice, campo, e.target.value)}
                    />
                    {falta ? <FieldError>Escribe el nombre del medicamento.</FieldError> : null}
                  </Field>
                )
              })}
            </div>
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          className="h-11 w-full"
          onClick={() => onCambio([...medicamentos, { ...VACIO }])}
        >
          <PlusIcon data-icon="inline-start" />
          Agregar medicamento
        </Button>
      </div>
    </FieldSet>
  )
}
