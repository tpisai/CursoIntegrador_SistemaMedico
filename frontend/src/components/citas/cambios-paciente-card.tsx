"use client"

import { CircleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { BotonConstanciaCambio } from "@/components/citas/boton-constancia-cambio"
import { EstadoSolicitudBadge } from "@/components/citas/estado-cita-badge"
import { useMisCambios } from "@/hooks/use-solicitudes"
import { mensajeDeError } from "@/lib/api/http"
import { formatDiaHora } from "@/lib/format"

// RF-17: seguimiento de los cambios de fecha que pidió el doctor y aprobó administración.
export function CambiosPacienteCard() {
  const { data, error, isLoading } = useMisCambios()

  if (!isLoading && !error && !data?.length) return null

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Cambios de fecha</CardTitle>
        <CardDescription>
          Tu doctor solicitó reprogramar estas citas. Administración las revisa y tú eliges el nuevo horario.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 2 }, (_, i) => (
              <Skeleton key={i} className="h-9" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : (
          <ul className="flex flex-col">
            {data?.map((cambio, i) => (
              <li key={cambio.idSolicitud} className="flex flex-col">
                {i > 0 ? <Separator /> : null}
                <div className="flex flex-col gap-2 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 flex-col gap-1">
                    <span className="flex flex-wrap items-center gap-2">
                      <span className="font-medium">{formatDiaHora(cambio.fechaCita, cambio.horaCita)}</span>
                      <EstadoSolicitudBadge estado={cambio.estado} />
                    </span>
                    {cambio.detalle ? (
                      <span className="text-muted-foreground">{cambio.detalle}</span>
                    ) : null}
                    <span className="text-xs text-muted-foreground">Motivo: {cambio.motivo}</span>
                    {cambio.respuestaAdmin ? (
                      <span className="text-xs text-muted-foreground">Administración: {cambio.respuestaAdmin}</span>
                    ) : null}
                  </div>
                  {cambio.constanciaDisponible ? <BotonConstanciaCambio idSolicitud={cambio.idSolicitud} /> : null}
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
