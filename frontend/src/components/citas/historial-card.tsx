"use client"

import { useState } from "react"
import { CircleAlertIcon, DownloadIcon, FileTextIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { useDocumentos, useHistorial } from "@/hooks/use-citas"
import { descargarDocumento } from "@/lib/api/citas"
import { mensajeDeError } from "@/lib/api/http"
import type { DocumentoMedico } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { formatFecha } from "@/lib/format"

function Cargando({ filas }: { filas: number }) {
  return (
    <div className="flex flex-col gap-2">
      {Array.from({ length: filas }, (_, i) => (
        <Skeleton key={i} className="h-8" />
      ))}
    </div>
  )
}

function ErrorCarga({ error }: { error: unknown }) {
  return (
    <Alert variant="destructive">
      <CircleAlertIcon />
      <AlertDescription>{mensajeDeError(error)}</AlertDescription>
    </Alert>
  )
}

// RF-09 (historial de asistencias) y RF-11 (descarga de documentos).
export function HistorialCard() {
  const historial = useHistorial()
  const documentos = useDocumentos()
  const [descargando, setDescargando] = useState<number | null>(null)

  async function descargar(doc: DocumentoMedico) {
    setDescargando(doc.idDocumento)
    try {
      guardarArchivo(await descargarDocumento(doc.idDocumento), doc.nombreArchivo)
    } catch (error) {
      toast.error(mensajeDeError(error))
    } finally {
      setDescargando(null)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Historial de atención</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        {historial.isLoading ? (
          <Cargando filas={3} />
        ) : historial.error ? (
          <ErrorCarga error={historial.error} />
        ) : historial.data?.length ? (
          <ul className="flex flex-col">
            {historial.data.map((atencion, i) => (
              <li key={atencion.idCita} className="flex flex-col">
                {i > 0 ? <Separator /> : null}
                <div className="flex items-center justify-between gap-3 py-2.5 text-sm">
                  <span>
                    {formatFecha(atencion.fecha)} · {atencion.especialidad}
                  </span>
                  <EstadoCitaBadge estado={atencion.estado} />
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted-foreground">Aún no tienes atenciones registradas.</p>
        )}

        <div className="flex flex-col gap-2">
          <h3 className="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            Documentos disponibles
          </h3>
          {documentos.isLoading ? (
            <Cargando filas={4} />
          ) : documentos.error ? (
            <ErrorCarga error={documentos.error} />
          ) : documentos.data?.length ? (
            <ul className="flex flex-col gap-2">
              {documentos.data.map((doc) => (
                <li
                  key={doc.idDocumento}
                  className="flex items-center justify-between gap-3 rounded-lg bg-muted/60 py-1 pr-1 pl-3 text-sm"
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <FileTextIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <span className="truncate">{doc.tipoDocumento}</span>
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-primary"
                    disabled={descargando === doc.idDocumento}
                    onClick={() => descargar(doc)}
                  >
                    {descargando === doc.idDocumento ? (
                      <Spinner data-icon="inline-start" />
                    ) : (
                      <DownloadIcon data-icon="inline-start" />
                    )}
                    PDF
                    <span className="sr-only">: descargar {doc.tipoDocumento}</span>
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-muted-foreground">
              Cuando el personal cargue tus resultados o recetas, podrás descargarlos aquí.
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
