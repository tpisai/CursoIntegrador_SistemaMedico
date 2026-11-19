"use client"

import { useState } from "react"
import { CircleAlertIcon, InboxIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { type Respuesta, ResponderSolicitudDialog } from "@/components/admin/responder-solicitud-dialog"
import { BotonConstanciaCambio, tieneConstanciaCambio } from "@/components/citas/boton-constancia-cambio"
import { EstadoSolicitudBadge } from "@/components/citas/estado-cita-badge"
import { useSolicitudesAdmin } from "@/hooks/use-admin"
import { mensajeDeError } from "@/lib/api/http"
import type { TipoSolicitud } from "@/lib/api/types"
import { formatDiaHora } from "@/lib/format"

const TEXTOS: Record<TipoSolicitud, { titulo: string; descripcion: string; vacio: string; aprobar: string }> = {
  CAMBIO_HORARIO: {
    titulo: "Solicitudes de cambio de fecha",
    descripcion:
      "Al aprobar, el paciente elige el nuevo horario en su portal; la constancia queda disponible para descargar.",
    vacio: "No hay cambios de fecha por revisar.",
    aprobar: "Aprobar y avisar al paciente",
  },
  DERIVACION: {
    titulo: "Solicitudes de derivación",
    descripcion: "Aprueba o rechaza las derivaciones solicitadas por los doctores hacia otros centros hospitalarios.",
    vacio: "No hay derivaciones por revisar.",
    aprobar: "Aprobar y notificar",
  },
}

export function SolicitudesAdminCard({ tipo }: { tipo: TipoSolicitud }) {
  const { data, error, isLoading } = useSolicitudesAdmin()
  const [respuesta, setRespuesta] = useState<Respuesta | null>(null)
  const solicitudes = data?.filter((s) => s.tipo === tipo)
  const textos = TEXTOS[tipo]

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">{textos.titulo}</CardTitle>
        <CardDescription>{textos.descripcion}</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-24" />
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !solicitudes?.length ? (
          <Empty className="border border-dashed p-6">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <InboxIcon />
              </EmptyMedia>
              <EmptyTitle>Todo al día</EmptyTitle>
              <EmptyDescription>{textos.vacio}</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <ul className="flex flex-col">
            {solicitudes.map((s, i) => (
              <li key={s.idSolicitud} className="flex flex-col">
                {i > 0 ? <Separator /> : null}
                <div className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 flex-col gap-1 text-sm">
                    <span className="flex flex-wrap items-center gap-2 font-semibold">
                      {s.doctor} · {s.especialidad}
                      <EstadoSolicitudBadge estado={s.estado} />
                    </span>
                    <span className="text-muted-foreground">
                      {s.paciente} (DNI {s.dniPaciente}) · {formatDiaHora(s.fechaCita, s.horaCita)}
                    </span>
                    {s.detalle ? <span className="text-xs text-muted-foreground">{s.detalle}</span> : null}
                    <span className="text-xs text-muted-foreground">Motivo: {s.motivo}</span>
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    {s.estado === "PENDIENTE" ? (
                      <>
                        <Button
                          size="sm"
                          onClick={() => setRespuesta({ solicitud: s, decision: "APROBAR", abierto: true })}
                        >
                          {textos.aprobar}
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => setRespuesta({ solicitud: s, decision: "RECHAZAR", abierto: true })}
                        >
                          Rechazar
                        </Button>
                      </>
                    ) : tieneConstanciaCambio(s.tipo, s.estado) ? (
                      <BotonConstanciaCambio idSolicitud={s.idSolicitud} />
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <ResponderSolicitudDialog
        respuesta={respuesta}
        onCerrar={() => setRespuesta((r) => r && { ...r, abierto: false })}
      />
    </Card>
  )
}
