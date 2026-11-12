"use client"

import { useState, useTransition } from "react"
import { CircleAlertIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { guardarAntecedentes } from "@/lib/api/consulta"
import { mensajeDeError } from "@/lib/api/http"
import type { AntecedentesRequest, PacienteConsulta } from "@/lib/api/types"

const CAMPOS: { campo: keyof AntecedentesRequest; etiqueta: string; ejemplo: string; maximo: number }[] = [
  { campo: "alergias", etiqueta: "Alergias", ejemplo: "Ej. Penicilina. Escribe \"Ninguna conocida\" si no tiene.", maximo: 1000 },
  { campo: "antecedentes", etiqueta: "Antecedentes y enfermedades", ejemplo: "Ej. Hipertensión arterial desde 2024. Sin cirugías.", maximo: 2000 },
  { campo: "medicacionHabitual", etiqueta: "Medicación habitual", ejemplo: "Ej. Losartán 50 mg cada 24 horas.", maximo: 1000 },
]

/** El doctor actualiza las alergias, los antecedentes y la medicación habitual del paciente. */
export function AntecedentesDialog({
  idCita,
  paciente,
  open,
  onOpenChange,
  onGuardado,
}: {
  idCita: number
  paciente: PacienteConsulta
  open: boolean
  onOpenChange: (open: boolean) => void
  onGuardado: () => Promise<unknown>
}) {
  const inicial = (): AntecedentesRequest => ({
    alergias: paciente.alergias ?? "",
    antecedentes: paciente.antecedentes ?? "",
    medicacionHabitual: paciente.medicacionHabitual ?? "",
  })
  const [datos, setDatos] = useState<AntecedentesRequest>(inicial)
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [guardando, startTransition] = useTransition()

  function cambiarApertura(abrir: boolean) {
    // Al abrir se parte siempre de lo que está guardado.
    if (abrir) {
      setDatos(inicial())
      setErrorApi(null)
    }
    onOpenChange(abrir)
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorApi(null)
    startTransition(async () => {
      try {
        await guardarAntecedentes(idCita, datos)
        await onGuardado()
        toast.success(`Antecedentes de ${paciente.nombreCompleto} actualizados.`)
        onOpenChange(false)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent className="max-h-[calc(100svh-2rem)] gap-5 overflow-y-auto sm:max-w-[520px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold">Antecedentes del paciente</DialogTitle>
          <DialogDescription>
            Los verá cualquier profesional del centro que atienda a {paciente.nombreCompleto}.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            {CAMPOS.map(({ campo, etiqueta, ejemplo, maximo }) => (
              <Field key={campo}>
                <FieldLabel htmlFor={`antecedentes-${campo}`}>{etiqueta}</FieldLabel>
                <Textarea
                  id={`antecedentes-${campo}`}
                  placeholder={ejemplo}
                  maxLength={maximo}
                  value={datos[campo]}
                  onChange={(e) => setDatos((d) => ({ ...d, [campo]: e.target.value }))}
                />
              </Field>
            ))}
            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}
          </FieldGroup>
          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" className="h-11" onClick={() => onOpenChange(false)} disabled={guardando}>
              Cancelar
            </Button>
            <Button type="submit" className="h-11" disabled={guardando}>
              {guardando ? <Spinner data-icon="inline-start" /> : null}
              Guardar antecedentes
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
