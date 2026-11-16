"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon, UserCheckIcon, UserXIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldGroup, FieldLabel, FieldTitle } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import {
  type Adjunto,
  AdjuntarDocumento,
  type ErroresAdjunto,
  validarAdjunto,
} from "@/components/consulta/adjuntar-documento"
import {
  RecetaMedicamentos,
  medicamentosParaEnviar,
  medicamentosSinNombre,
} from "@/components/consulta/receta-medicamentos"
import { esClaveDelDoctor } from "@/lib/api/consulta"
import { registrarAtencion, subirDocumento } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { CitaAsignada, MedicamentoRequest } from "@/lib/api/types"

type Ficha = {
  anamnesis: string
  examenFisico: string
  diagnostico: string
  tratamiento: string
  observaciones: string
}
type Errores = Partial<Record<keyof Ficha, string>> & ErroresAdjunto

const CAMPOS: { campo: keyof Ficha; etiqueta: string; ejemplo: string }[] = [
  { campo: "anamnesis", etiqueta: "Síntomas", ejemplo: "Ej. Dolor de cabeza desde hace 3 días, sin fiebre." },
  { campo: "examenFisico", etiqueta: "Exploración física", ejemplo: "Ej. PA 120/80 mmHg, FC 76 lpm, afebril." },
  { campo: "diagnostico", etiqueta: "Diagnóstico", ejemplo: "Ej. Cefalea tensional." },
  { campo: "tratamiento", etiqueta: "Tratamiento", ejemplo: "Ej. Reposo, hidratación y analgésico por 3 días." },
  { campo: "observaciones", etiqueta: "Observaciones", ejemplo: "Ej. Control en una semana." },
]

// Ficha de consulta médica (RF-10): síntomas, exploración, diagnóstico, tratamiento y receta.
export function FichaConsulta({ cita }: { cita: CitaAsignada }) {
  const { mutate } = useSWRConfig()
  const [asistio, setAsistio] = useState(true)
  const [ficha, setFicha] = useState<Ficha>({
    anamnesis: "",
    examenFisico: "",
    diagnostico: "",
    tratamiento: "",
    observaciones: "",
  })
  const [medicamentos, setMedicamentos] = useState<MedicamentoRequest[]>([])
  const [adjunto, setAdjunto] = useState<Adjunto>({ tipoDocumento: "", archivo: null })
  const [errores, setErrores] = useState<Errores>({})
  const [sinNombre, setSinNombre] = useState<number[]>([])
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [guardando, startTransition] = useTransition()

  function actualizar(campo: keyof Ficha, valor: string) {
    setFicha((f) => ({ ...f, [campo]: valor }))
    // El aviso de un campo desaparece en cuanto se corrige.
    setErrores((e) => ({ ...e, [campo]: undefined }))
  }

  function validar(): boolean {
    if (!asistio) return true
    const nuevos: Errores = validarAdjunto(adjunto)
    if (ficha.diagnostico.trim().length < 3) nuevos.diagnostico = "Escribe el diagnóstico."
    if (ficha.tratamiento.trim().length < 3) nuevos.tratamiento = "Escribe el tratamiento indicado."
    const faltanNombres = medicamentosSinNombre(medicamentos)
    setErrores(nuevos)
    setSinNombre(faltanNombres)
    return Object.keys(nuevos).length === 0 && faltanNombres.length === 0
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!validar()) return
    setErrorApi(null)

    startTransition(async () => {
      try {
        const resultado = await registrarAtencion(
          cita.idCita,
          asistio ? { asistio: true, ...ficha, medicamentos: medicamentosParaEnviar(medicamentos) } : { asistio: false }
        )
        if (resultado.idAtencion && adjunto.archivo) {
          try {
            await subirDocumento(resultado.idAtencion, adjunto.tipoDocumento, adjunto.archivo)
          } catch (error) {
            toast.warning(`La atención se guardó, pero el documento no se subió: ${mensajeDeError(error)}`)
          }
        }
        toast.success(
          asistio
            ? `Atención de ${cita.paciente} registrada en su historia clínica.`
            : `Registraste que ${cita.paciente} no asistió.`
        )
        // Al recargar, la página pasa a mostrar la atención ya registrada.
        await Promise.all([mutate(esClaveDelDoctor), mutate(RUTA_NOTIFICACIONES)])
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Ficha de consulta</CardTitle>
        <CardDescription>Lo que registres queda en la historia clínica del paciente.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-5">
            <Field>
              <FieldTitle id="ficha-asistio">¿El paciente asistió?</FieldTitle>
              <ToggleGroup
                type="single"
                variant="slot"
                size="lg"
                spacing={2}
                aria-labelledby="ficha-asistio"
                className="grid w-full grid-cols-2 sm:max-w-sm"
                value={asistio ? "SI" : "NO"}
                onValueChange={(valor) => valor && setAsistio(valor === "SI")}
              >
                <ToggleGroupItem value="SI" className="h-11 w-full">
                  <UserCheckIcon data-icon="inline-start" />
                  Asistió
                </ToggleGroupItem>
                <ToggleGroupItem value="NO" className="h-11 w-full">
                  <UserXIcon data-icon="inline-start" />
                  No asistió
                </ToggleGroupItem>
              </ToggleGroup>
            </Field>

            {asistio ? (
              <>
                {CAMPOS.map(({ campo, etiqueta, ejemplo }) => (
                  <Field key={campo} data-invalid={!!errores[campo]}>
                    <FieldLabel htmlFor={`ficha-${campo}`}>{etiqueta}</FieldLabel>
                    <Textarea
                      id={`ficha-${campo}`}
                      placeholder={ejemplo}
                      maxLength={2000}
                      aria-invalid={!!errores[campo]}
                      value={ficha[campo]}
                      onChange={(e) => actualizar(campo, e.target.value)}
                    />
                    <FieldError>{errores[campo]}</FieldError>
                  </Field>
                ))}
                <RecetaMedicamentos
                  medicamentos={medicamentos}
                  sinNombre={sinNombre}
                  onCambio={(nuevos) => {
                    setMedicamentos(nuevos)
                    setSinNombre([])
                  }}
                />
                <AdjuntarDocumento
                  id="ficha-adjunto"
                  descripcion="Resultados, informe u otro archivo. El paciente podrá descargarlo."
                  adjunto={adjunto}
                  errores={errores}
                  onCambio={(cambios) => {
                    setAdjunto((a) => ({ ...a, ...cambios }))
                    setErrores((e) => ({ ...e, tipoDocumento: undefined, archivo: undefined }))
                  }}
                />
              </>
            ) : (
              <Alert variant="warning">
                <UserXIcon />
                <AlertDescription>
                  La cita quedará como &quot;No asistió&quot; en el historial del paciente y cuenta para el indicador de
                  ausentismo.
                </AlertDescription>
              </Alert>
            )}

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="w-full sm:w-auto sm:self-end" disabled={guardando}>
              {guardando ? <Spinner data-icon="inline-start" /> : null}
              {asistio ? "Guardar atención" : "Registrar inasistencia"}
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
