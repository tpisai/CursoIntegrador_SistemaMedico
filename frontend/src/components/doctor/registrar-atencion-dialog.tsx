"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon, UserCheckIcon, UserXIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldSet, FieldLegend, FieldTitle } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { CampoSelect } from "@/components/portal/campo-select"
import { useTiposDocumento } from "@/hooks/use-doctor"
import { registrarAtencion, subirDocumento } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { CitaAsignada } from "@/lib/api/types"
import { formatDiaHora } from "@/lib/format"

const MAX_ARCHIVO = 5 * 1024 * 1024
const TIPOS_ARCHIVO = ["application/pdf", "image/jpeg", "image/png"]

type Datos = {
  asistio: "SI" | "NO"
  anamnesis: string
  examenFisico: string
  diagnostico: string
  tratamiento: string
  observaciones: string
  tipoDocumento: string
  archivo: File | null
}

const VACIO: Datos = {
  asistio: "SI",
  anamnesis: "",
  examenFisico: "",
  diagnostico: "",
  tratamiento: "",
  observaciones: "",
  tipoDocumento: "",
  archivo: null,
}

type Errores = Partial<Record<keyof Datos, string>>

function validar(d: Datos): Errores {
  const errores: Errores = {}
  if (d.asistio === "NO") return errores
  if (d.diagnostico.trim().length < 3) errores.diagnostico = "Escribe el diagnóstico."
  if (d.tratamiento.trim().length < 3) errores.tratamiento = "Escribe el tratamiento indicado."
  if (d.archivo) {
    if (!d.tipoDocumento) errores.tipoDocumento = "Elige qué tipo de documento adjuntas."
    if (!TIPOS_ARCHIVO.includes(d.archivo.type)) errores.archivo = "Solo PDF, JPG o PNG."
    else if (d.archivo.size > MAX_ARCHIVO) errores.archivo = "El archivo supera 5 MB."
  }
  return errores
}

// Formulario RF-10: registra la atención (tabla atencion) o la inasistencia, y adjunta un documento.
export function RegistrarAtencionDialog({
  cita,
  abierto,
  onCerrar,
}: {
  cita: CitaAsignada | null
  abierto: boolean
  onCerrar: () => void
}) {
  const { mutate } = useSWRConfig()
  const tipos = useTiposDocumento()
  const [datos, setDatos] = useState<Datos>(VACIO)
  const [errores, setErrores] = useState<Errores>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  // Cambiar la clave vacía el campo de archivo, que no se puede limpiar por código.
  const [claveArchivo, setClaveArchivo] = useState(0)
  const [guardando, startTransition] = useTransition()
  const asistio = datos.asistio === "SI"

  function actualizar<K extends keyof Datos>(campo: K, valor: Datos[K]) {
    setDatos((d) => ({ ...d, [campo]: valor }))
    // El aviso de un campo desaparece en cuanto se corrige.
    setErrores((e) => ({ ...e, [campo]: undefined }))
  }

  function cerrar() {
    setDatos(VACIO)
    setErrores({})
    setErrorApi(null)
    setClaveArchivo((k) => k + 1)
    onCerrar()
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!cita) return
    const nuevos = validar(datos)
    setErrores(nuevos)
    if (Object.keys(nuevos).length) return
    setErrorApi(null)

    startTransition(async () => {
      try {
        const resultado = await registrarAtencion(
          cita.idCita,
          asistio
            ? {
                asistio: true,
                anamnesis: datos.anamnesis,
                examenFisico: datos.examenFisico,
                diagnostico: datos.diagnostico,
                tratamiento: datos.tratamiento,
                observaciones: datos.observaciones,
              }
            : { asistio: false }
        )
        if (resultado.idAtencion && datos.archivo) {
          try {
            await subirDocumento(resultado.idAtencion, datos.tipoDocumento, datos.archivo)
          } catch (error) {
            toast.warning(`La atención se guardó, pero el documento no se subió: ${mensajeDeError(error)}`)
          }
        }
        toast.success(
          asistio
            ? `Atención de ${cita.paciente} registrada en su historia clínica.`
            : `Registraste que ${cita.paciente} no asistió.`
        )
        await Promise.all([
          mutate((clave) => typeof clave === "string" && clave.startsWith("/doctor/citas")),
          mutate(RUTA_NOTIFICACIONES),
        ])
        cerrar()
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={abierto} onOpenChange={(abrir) => !abrir && cerrar()}>
      {cita ? (
        <DialogContent className="max-h-[calc(100svh-2rem)] gap-5 overflow-y-auto sm:max-w-[560px] sm:p-8">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold">Registrar atención · {cita.paciente}</DialogTitle>
            <DialogDescription>
              DNI {cita.dniPaciente} · {formatDiaHora(cita.fecha, cita.horaInicio)} · Ticket {cita.numeroTicket}
              {cita.motivo ? ` · Motivo: ${cita.motivo}` : ""}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={onSubmit} noValidate>
            <FieldGroup className="gap-4">
              <Field>
                <FieldTitle id="atencion-asistio">¿El paciente asistió?</FieldTitle>
                <ToggleGroup
                  type="single"
                  variant="slot"
                  size="lg"
                  spacing={2}
                  aria-labelledby="atencion-asistio"
                  className="grid w-full grid-cols-2"
                  value={datos.asistio}
                  onValueChange={(v) => v && actualizar("asistio", v as Datos["asistio"])}
                >
                  <ToggleGroupItem value="SI" className="w-full">
                    <UserCheckIcon data-icon="inline-start" />
                    Asistió
                  </ToggleGroupItem>
                  <ToggleGroupItem value="NO" className="w-full">
                    <UserXIcon data-icon="inline-start" />
                    No asistió
                  </ToggleGroupItem>
                </ToggleGroup>
              </Field>

              {asistio ? (
                <>
                  <Field>
                    <FieldLabel htmlFor="atencion-anamnesis">Motivo de consulta y síntomas</FieldLabel>
                    <Textarea
                      id="atencion-anamnesis"
                      placeholder="Ej. Dolor de cabeza desde hace 3 días, sin fiebre."
                      maxLength={2000}
                      value={datos.anamnesis}
                      onChange={(e) => actualizar("anamnesis", e.target.value)}
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="atencion-examen">Examen físico</FieldLabel>
                    <Textarea
                      id="atencion-examen"
                      placeholder="Ej. PA 120/80 mmHg, FC 76 lpm, afebril."
                      maxLength={2000}
                      value={datos.examenFisico}
                      onChange={(e) => actualizar("examenFisico", e.target.value)}
                    />
                  </Field>
                  <Field data-invalid={!!errores.diagnostico}>
                    <FieldLabel htmlFor="atencion-diagnostico">Diagnóstico</FieldLabel>
                    <Textarea
                      id="atencion-diagnostico"
                      placeholder="Ej. Cefalea tensional."
                      maxLength={2000}
                      aria-invalid={!!errores.diagnostico}
                      value={datos.diagnostico}
                      onChange={(e) => actualizar("diagnostico", e.target.value)}
                    />
                    <FieldError>{errores.diagnostico}</FieldError>
                  </Field>
                  <Field data-invalid={!!errores.tratamiento}>
                    <FieldLabel htmlFor="atencion-tratamiento">Tratamiento</FieldLabel>
                    <Textarea
                      id="atencion-tratamiento"
                      placeholder="Ej. Paracetamol 500 mg cada 8 horas por 3 días."
                      maxLength={2000}
                      aria-invalid={!!errores.tratamiento}
                      value={datos.tratamiento}
                      onChange={(e) => actualizar("tratamiento", e.target.value)}
                    />
                    <FieldError>{errores.tratamiento}</FieldError>
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="atencion-observaciones">Observaciones</FieldLabel>
                    <Textarea
                      id="atencion-observaciones"
                      placeholder="Ej. Control en una semana."
                      maxLength={2000}
                      value={datos.observaciones}
                      onChange={(e) => actualizar("observaciones", e.target.value)}
                    />
                  </Field>

                  <FieldSet>
                    <FieldLegend variant="label">Documento adjunto (opcional)</FieldLegend>
                    <FieldDescription>Receta, resultados o informe. El paciente podrá descargarlo.</FieldDescription>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <CampoSelect
                        id="atencion-tipo-documento"
                        etiqueta="Tipo de documento"
                        placeholder="Elige el tipo"
                        opciones={tipos.data?.map((t) => ({ valor: t, texto: t }))}
                        valor={datos.tipoDocumento}
                        onCambio={(v) => actualizar("tipoDocumento", v)}
                        error={errores.tipoDocumento}
                      />
                      <Field data-invalid={!!errores.archivo}>
                        <FieldLabel htmlFor="atencion-archivo">Archivo (PDF, JPG o PNG, hasta 5 MB)</FieldLabel>
                        <Input
                          key={claveArchivo}
                          id="atencion-archivo"
                          type="file"
                          accept="application/pdf,image/jpeg,image/png"
                          className="h-10"
                          aria-invalid={!!errores.archivo}
                          onChange={(e) => actualizar("archivo", e.target.files?.[0] ?? null)}
                        />
                        <FieldError>{errores.archivo}</FieldError>
                      </Field>
                    </div>
                  </FieldSet>
                </>
              ) : (
                <Alert variant="warning">
                  <UserXIcon />
                  <AlertDescription>
                    La cita quedará como &quot;No asistió&quot; en el historial del paciente y cuenta para el
                    indicador de ausentismo.
                  </AlertDescription>
                </Alert>
              )}

              {errorApi ? (
                <Alert variant="destructive">
                  <CircleAlertIcon />
                  <AlertDescription>{errorApi}</AlertDescription>
                </Alert>
              ) : null}
            </FieldGroup>

            <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" size="lg" onClick={cerrar} disabled={guardando}>
                Cancelar
              </Button>
              <Button type="submit" size="lg" disabled={guardando}>
                {guardando ? <Spinner data-icon="inline-start" /> : null}
                {asistio ? "Guardar atención" : "Registrar inasistencia"}
              </Button>
            </div>
          </form>
        </DialogContent>
      ) : null}
    </Dialog>
  )
}
