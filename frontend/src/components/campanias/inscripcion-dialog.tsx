"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CheckIcon, CircleAlertIcon, DownloadIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Spinner } from "@/components/ui/spinner"
import { descargarComprobante, inscribirse, RUTA_CAMPANIAS } from "@/lib/api/campanias"
import { mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { Campania, Inscripcion } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { formatRango } from "@/lib/format"

export type EstadoInscripcion = { campania: Campania; abierto: boolean }

export async function bajarComprobante(idInscripcion: number, codigo: string) {
  try {
    guardarArchivo(await descargarComprobante(idInscripcion), `comprobante-${codigo}.pdf`)
  } catch (error) {
    toast.error(mensajeDeError(error))
  }
}

// Formulario RF-21: el paciente se inscribe (tabla inscripcion_campania) y recibe su comprobante digital.
export function InscripcionDialog({ estado, onCerrar }: { estado: EstadoInscripcion | null; onCerrar: () => void }) {
  const { mutate } = useSWRConfig()
  const [acepta, setAcepta] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [inscripcion, setInscripcion] = useState<Inscripcion | null>(null)
  const [enviando, startTransition] = useTransition()
  const [descargando, startDescarga] = useTransition()
  const campania = estado?.campania

  function cerrar() {
    setAcepta(false)
    setError(null)
    setErrorApi(null)
    setInscripcion(null)
    onCerrar()
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!campania) return
    if (!acepta) {
      setError("Confirma que asistirás con tu DNI.")
      return
    }
    setError(null)
    setErrorApi(null)
    startTransition(async () => {
      try {
        setInscripcion(await inscribirse(campania.idCampania))
        toast.success(`Te inscribiste en ${campania.titulo}.`)
        await Promise.all([mutate(RUTA_CAMPANIAS), mutate(RUTA_NOTIFICACIONES)])
      } catch (e) {
        setErrorApi(mensajeDeError(e))
        // Si se acabaron los cupos mientras decidía, la lista debe mostrarlo.
        await mutate(RUTA_CAMPANIAS)
      }
    })
  }

  return (
    <Dialog open={estado?.abierto ?? false} onOpenChange={(abrir) => !abrir && cerrar()}>
      {campania ? (
        <DialogContent className="gap-5 sm:max-w-[460px] sm:p-8">
          {inscripcion ? (
            <>
              <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-success text-success-foreground">
                <CheckIcon className="size-7" strokeWidth={3} aria-hidden="true" />
              </div>
              <DialogHeader className="items-center text-center">
                <DialogTitle className="text-xl font-bold">¡Inscripción confirmada!</DialogTitle>
                <DialogDescription>{campania.titulo}</DialogDescription>
              </DialogHeader>
              <div className="flex flex-col items-center gap-1 rounded-xl bg-secondary px-6 py-4 text-center">
                <span className="text-[11px] font-semibold tracking-wider text-secondary-foreground uppercase">
                  Comprobante de inscripción
                </span>
                <span className="text-3xl font-bold tracking-tight text-primary">N.° {inscripcion.codigo}</span>
              </div>
              <p className="text-center text-sm text-muted-foreground">
                {formatRango(campania.fechaInicio, campania.fechaFin, campania.hora)}
                {campania.lugar ? ` · ${campania.lugar}` : ""}. Presenta tu comprobante y tu DNI.
              </p>
              <div className="flex flex-col gap-2">
                <DialogClose asChild>
                  <Button size="xl" className="w-full">
                    Entendido
                  </Button>
                </DialogClose>
                <Button
                  variant="link"
                  disabled={descargando}
                  onClick={() => startDescarga(() => bajarComprobante(inscripcion.idInscripcion, inscripcion.codigo))}
                >
                  {descargando ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
                  Descargar comprobante (PDF)
                </Button>
              </div>
            </>
          ) : (
            <>
              <DialogHeader>
                <DialogTitle className="text-xl font-bold">Inscribirme en la campaña</DialogTitle>
                <DialogDescription>{campania.titulo}</DialogDescription>
              </DialogHeader>
              <form onSubmit={onSubmit} noValidate>
                <FieldGroup className="gap-4">
                  <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 rounded-lg bg-muted/60 p-4 text-sm">
                    <dt className="text-muted-foreground">Fecha</dt>
                    <dd>{formatRango(campania.fechaInicio, campania.fechaFin, campania.hora)}</dd>
                    <dt className="text-muted-foreground">Lugar</dt>
                    <dd>{campania.lugar ?? "Centro de Salud Miguel Grau"}</dd>
                    <dt className="text-muted-foreground">Cupos libres</dt>
                    <dd>
                      {campania.cuposDisponibles} de {campania.cupos}
                    </dd>
                  </dl>
                  <Field orientation="horizontal" data-invalid={!!error}>
                    <Checkbox
                      id="inscripcion-acepta"
                      checked={acepta}
                      aria-invalid={!!error}
                      onCheckedChange={(v) => {
                        setAcepta(v === true)
                        setError(null)
                      }}
                    />
                    <div className="flex flex-col gap-1">
                      <FieldLabel htmlFor="inscripcion-acepta" className="leading-snug font-normal">
                        Asistiré en la fecha indicada y presentaré mi DNI. Si no puedo ir, avisaré al centro de salud.
                      </FieldLabel>
                      <FieldError>{error}</FieldError>
                    </div>
                  </Field>
                  {errorApi ? (
                    <Alert variant="destructive">
                      <CircleAlertIcon />
                      <AlertDescription>{errorApi}</AlertDescription>
                    </Alert>
                  ) : null}
                  <Button type="submit" size="xl" className="w-full" disabled={enviando}>
                    {enviando ? <Spinner data-icon="inline-start" /> : null}
                    Confirmar inscripción
                  </Button>
                </FieldGroup>
              </form>
            </>
          )}
        </DialogContent>
      ) : null}
    </Dialog>
  )
}
