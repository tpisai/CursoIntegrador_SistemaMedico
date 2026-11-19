"use client"

import { useState } from "react"
import { CircleAlertIcon, DownloadIcon, InboxIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { useHistorial } from "@/hooks/use-citas"
import { descargarReferenciaPaciente } from "@/lib/api/citas"
import { mensajeDeError } from "@/lib/api/http"
import { guardarArchivo } from "@/lib/descargar"
import { formatFecha } from "@/lib/format"

export function ReferenciasDerivacionCard() {
  const historial = useHistorial()
  const [descargando, setDescargando] = useState<number | null>(null)
  const referencias = historial.data?.filter((atencion) => atencion.tieneDerivacion && atencion.idAtencion !== null)

  async function descargar(idAtencion: number) {
    setDescargando(idAtencion)
    try {
      guardarArchivo(await descargarReferenciaPaciente(idAtencion), `referencia-${idAtencion}.pdf`)
    } catch (error) {
      toast.error(mensajeDeError(error))
    } finally {
      setDescargando(null)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Referencias de derivación</CardTitle>
        <CardDescription>Descarga las referencias emitidas durante tus atenciones.</CardDescription>
      </CardHeader>
      <CardContent>
        {historial.isLoading ? (
          <Skeleton className="h-16" />
        ) : historial.error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(historial.error)}</AlertDescription>
          </Alert>
        ) : referencias?.length ? (
          <ul className="flex flex-col divide-y">
            {referencias.map((atencion) => (
              <li key={atencion.idAtencion} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <span className="text-sm">
                  {formatFecha(atencion.fecha)} · {atencion.especialidad} · {atencion.doctor}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={descargando === atencion.idAtencion}
                  onClick={() => atencion.idAtencion !== null && descargar(atencion.idAtencion)}
                >
                  {descargando === atencion.idAtencion ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
                  Descargar PDF
                </Button>
              </li>
            ))}
          </ul>
        ) : (
          <Empty className="border border-dashed p-6">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <InboxIcon />
              </EmptyMedia>
              <EmptyTitle>Aún no tienes referencias</EmptyTitle>
              <EmptyDescription>Las referencias disponibles aparecerán aquí.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
      </CardContent>
    </Card>
  )
}
