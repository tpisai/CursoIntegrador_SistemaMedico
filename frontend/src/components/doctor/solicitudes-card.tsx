"use client"

import { CircleAlertIcon, InboxIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { EstadoSolicitudBadge } from "@/components/citas/estado-cita-badge"
import { useSolicitudes } from "@/hooks/use-doctor"
import { mensajeDeError } from "@/lib/api/http"
import { formatDiaHora, tiempoRelativo } from "@/lib/format"

const TIPOS = { CAMBIO_HORARIO: "Cambio de fecha", DERIVACION: "Derivación" } as const

export function SolicitudesCard() {
  const { data: solicitudes, error, isLoading } = useSolicitudes()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Mis solicitudes</CardTitle>
        <CardDescription>Cambios de fecha y derivaciones enviados a administración.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-10" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !solicitudes?.length ? (
          <Empty className="border border-dashed p-8">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <InboxIcon />
              </EmptyMedia>
              <EmptyTitle>Aún no envías solicitudes</EmptyTitle>
              <EmptyDescription>Desde el inicio del panel puedes pedir un cambio de fecha o derivar a un paciente.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tipo</TableHead>
                <TableHead>Paciente</TableHead>
                <TableHead>Cita</TableHead>
                <TableHead>Detalle</TableHead>
                <TableHead>Motivo</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Enviada</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {solicitudes.map((s) => (
                <TableRow key={s.idSolicitud}>
                  <TableCell className="font-medium">{TIPOS[s.tipo]}</TableCell>
                  <TableCell>{s.paciente}</TableCell>
                  <TableCell>{formatDiaHora(s.fechaCita, s.horaCita)}</TableCell>
                  <TableCell>{s.detalle ?? "—"}</TableCell>
                  <TableCell className="max-w-64 truncate" title={s.motivo}>
                    {s.motivo}
                  </TableCell>
                  <TableCell>
                    <EstadoSolicitudBadge estado={s.estado} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{tiempoRelativo(s.fechaSolicitud)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
