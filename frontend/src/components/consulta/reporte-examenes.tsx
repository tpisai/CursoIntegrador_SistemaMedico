"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon, PlusIcon, Trash2Icon, UserCheckIcon, UserXIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
  FieldTitle,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import {
  type Adjunto,
  AdjuntarDocumento,
  type ErroresAdjunto,
  validarAdjunto,
} from "@/components/consulta/adjuntar-documento"
import { esClaveDelDoctor, registrarResultados } from "@/lib/api/consulta"
import { registrarAtencion, subirDocumento } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { CitaAsignada, ResultadoExamenRequest } from "@/lib/api/types"

type Fila = Required<ResultadoExamenRequest>
type Errores = { conclusion?: string; resultados?: string } & ErroresAdjunto

const FILA_VACIA: Fila = { tipoExamen: "", resultado: "", valorReferencia: "", interpretacion: "" }

const COLUMNAS: { campo: keyof Fila; etiqueta: string; ejemplo: string }[] = [
  { campo: "tipoExamen", etiqueta: "Tipo de examen", ejemplo: "Ej. Hemoglobina" },
  { campo: "resultado", etiqueta: "Resultado", ejemplo: "Ej. 13.4 g/dL" },
  { campo: "valorReferencia", etiqueta: "Valor de referencia", ejemplo: "Ej. 12.0 a 16.0 g/dL" },
  { campo: "interpretacion", etiqueta: "Interpretación", ejemplo: "Ej. Normal" },
]

const tieneDatos = (fila: Fila) => Object.values(fila).some((valor) => valor.trim() !== "")

/**
 * Reporte de exámenes de laboratorio, ecografía y rayos X: tipo de examen, resultado, valor de
 * referencia e interpretación. Con idAtencion solo agrega los resultados a una atención ya guardada.
 */
export function ReporteExamenes({ cita, idAtencion }: { cita: CitaAsignada; idAtencion?: number }) {
  const { mutate } = useSWRConfig()
  const soloResultados = idAtencion !== undefined
  const [asistio, setAsistio] = useState(true)
  const [solicitado, setSolicitado] = useState(cita.motivo ?? "")
  const [filas, setFilas] = useState<Fila[]>([{ ...FILA_VACIA }])
  const [conclusion, setConclusion] = useState("")
  const [observaciones, setObservaciones] = useState("")
  const [adjunto, setAdjunto] = useState<Adjunto>({ tipoDocumento: "", archivo: null })
  const [errores, setErrores] = useState<Errores>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [guardando, startTransition] = useTransition()

  function actualizarFila(indice: number, campo: keyof Fila, valor: string) {
    setFilas((actuales) => actuales.map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)))
    setErrores((e) => ({ ...e, resultados: undefined }))
  }

  function validar(conDatos: Fila[]): boolean {
    if (!asistio) return true
    const nuevos: Errores = soloResultados ? {} : validarAdjunto(adjunto)
    if (!conDatos.length) nuevos.resultados = "Agrega al menos un examen con su resultado."
    else if (conDatos.some((fila) => !fila.tipoExamen.trim() || !fila.resultado.trim())) {
      nuevos.resultados = "Cada examen necesita su tipo y su resultado."
    }
    if (!soloResultados && conclusion.trim().length < 3) nuevos.conclusion = "Escribe la conclusión del examen."
    setErrores(nuevos)
    return Object.keys(nuevos).length === 0
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const conDatos = filas.filter(tieneDatos)
    if (!validar(conDatos)) return
    setErrorApi(null)

    startTransition(async () => {
      try {
        let atencion = idAtencion ?? null
        if (!soloResultados) {
          const resultado = await registrarAtencion(
            cita.idCita,
            asistio
              ? { asistio: true, anamnesis: solicitado, diagnostico: conclusion, observaciones }
              : { asistio: false }
          )
          atencion = resultado.idAtencion
        }
        if (atencion) {
          try {
            await registrarResultados(atencion, conDatos)
            if (adjunto.archivo) await subirDocumento(atencion, adjunto.tipoDocumento, adjunto.archivo)
          } catch (error) {
            // La atención ya quedó guardada: la página permite completar lo que faltó.
            toast.warning(`La atención se guardó, pero faltó completar el reporte: ${mensajeDeError(error)}`)
          }
        }
        toast.success(
          asistio
            ? `Reporte de exámenes de ${cita.paciente} guardado en su historia clínica.`
            : `Registraste que ${cita.paciente} no asistió.`
        )
        await Promise.all([mutate(esClaveDelDoctor), mutate(RUTA_NOTIFICACIONES)])
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{soloResultados ? "Completar los resultados" : "Reporte de exámenes"}</CardTitle>
        <CardDescription>
          {soloResultados
            ? "La atención ya está guardada; faltan los resultados de los exámenes."
            : "Los resultados quedan en la historia clínica y el médico tratante los verá en su consulta."}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-5">
            {soloResultados ? null : (
              <Field>
                <FieldTitle id="reporte-asistio">¿El paciente asistió?</FieldTitle>
                <ToggleGroup
                  type="single"
                  variant="slot"
                  size="lg"
                  spacing={2}
                  aria-labelledby="reporte-asistio"
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
            )}

            {asistio ? (
              <>
                {soloResultados ? null : (
                  <Field>
                    <FieldLabel htmlFor="reporte-solicitado">Examen solicitado</FieldLabel>
                    <Input
                      id="reporte-solicitado"
                      className="h-11"
                      placeholder="Ej. Hemograma completo y glucosa en ayunas"
                      maxLength={500}
                      value={solicitado}
                      onChange={(e) => setSolicitado(e.target.value)}
                    />
                  </Field>
                )}

                <FieldSet data-invalid={!!errores.resultados}>
                  <FieldLegend variant="label">Resultados</FieldLegend>
                  <FieldDescription>Registra cada examen con su resultado y su valor de referencia.</FieldDescription>
                  <div className="flex flex-col gap-3">
                    {filas.map((fila, indice) => (
                      <div key={indice} className="flex flex-col gap-3 rounded-lg border p-4">
                        <div className="flex items-center justify-between gap-3">
                          <span className="font-medium">Examen {indice + 1}</span>
                          {filas.length > 1 ? (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-lg"
                              onClick={() => setFilas((actuales) => actuales.filter((_, i) => i !== indice))}
                            >
                              <Trash2Icon />
                              <span className="sr-only">Quitar el examen {indice + 1}</span>
                            </Button>
                          ) : null}
                        </div>
                        <div className="grid gap-3 sm:grid-cols-2">
                          {COLUMNAS.map(({ campo, etiqueta, ejemplo }) => (
                            <Field key={campo}>
                              <FieldLabel htmlFor={`examen-${indice}-${campo}`}>{etiqueta}</FieldLabel>
                              <Input
                                id={`examen-${indice}-${campo}`}
                                className="h-11"
                                placeholder={ejemplo}
                                maxLength={campo === "interpretacion" ? 1000 : 150}
                                value={fila[campo]}
                                onChange={(e) => actualizarFila(indice, campo, e.target.value)}
                              />
                            </Field>
                          ))}
                        </div>
                      </div>
                    ))}
                    <FieldError>{errores.resultados}</FieldError>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-11 w-full"
                      onClick={() => setFilas((actuales) => [...actuales, { ...FILA_VACIA }])}
                    >
                      <PlusIcon data-icon="inline-start" />
                      Agregar examen
                    </Button>
                  </div>
                </FieldSet>

                {soloResultados ? null : (
                  <>
                    <Field data-invalid={!!errores.conclusion}>
                      <FieldLabel htmlFor="reporte-conclusion">Conclusión</FieldLabel>
                      <Textarea
                        id="reporte-conclusion"
                        placeholder="Ej. Hemograma dentro de los valores de referencia."
                        maxLength={2000}
                        aria-invalid={!!errores.conclusion}
                        value={conclusion}
                        onChange={(e) => {
                          setConclusion(e.target.value)
                          setErrores((er) => ({ ...er, conclusion: undefined }))
                        }}
                      />
                      <FieldError>{errores.conclusion}</FieldError>
                    </Field>
                    <Field>
                      <FieldLabel htmlFor="reporte-observaciones">Observaciones</FieldLabel>
                      <Textarea
                        id="reporte-observaciones"
                        placeholder="Ej. Muestra tomada en ayunas."
                        maxLength={2000}
                        value={observaciones}
                        onChange={(e) => setObservaciones(e.target.value)}
                      />
                    </Field>
                    <AdjuntarDocumento
                      id="reporte-adjunto"
                      descripcion="El informe o la imagen del examen. El paciente podrá descargarlo."
                      adjunto={adjunto}
                      errores={errores}
                      onCambio={(cambios) => {
                        setAdjunto((a) => ({ ...a, ...cambios }))
                        setErrores((e) => ({ ...e, tipoDocumento: undefined, archivo: undefined }))
                      }}
                    />
                  </>
                )}
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
              {!asistio ? "Registrar inasistencia" : soloResultados ? "Guardar resultados" : "Guardar reporte de exámenes"}
            </Button>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
