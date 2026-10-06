"use client"

import { CircleAlertIcon, MegaphoneIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { EstadoCampaniaBadge } from "@/components/campanias/estado-campania-badge"
import { useCampaniasAdmin } from "@/hooks/use-admin"
import { mensajeDeError } from "@/lib/api/http"
import { formatRango } from "@/lib/format"

export function CampaniasAdminCard() {
  const { data: campanias, error, isLoading } = useCampaniasAdmin()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-semibold">Campañas publicadas</CardTitle>
        <CardDescription>Inscritos y cupos se actualizan con cada inscripción de los pacientes.</CardDescription>
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
        ) : !campanias?.length ? (
          <Empty className="border border-dashed p-8">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <MegaphoneIcon />
              </EmptyMedia>
              <EmptyTitle>Aún no hay campañas</EmptyTitle>
              <EmptyDescription>Publica la primera con el botón Nueva campaña.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Campaña</TableHead>
                <TableHead>Fecha</TableHead>
                <TableHead className="text-right">Inscritos</TableHead>
                <TableHead className="text-right">Cupos libres</TableHead>
                <TableHead>Estado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {campanias.map((c) => (
                <TableRow key={c.idCampania}>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium">{c.titulo}</span>
                      {c.lugar ? <span className="text-xs text-muted-foreground">{c.lugar}</span> : null}
                    </div>
                  </TableCell>
                  <TableCell>{formatRango(c.fechaInicio, c.fechaFin, c.hora)}</TableCell>
                  <TableCell className="text-right tabular-nums">{c.inscritos}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {c.cuposDisponibles} de {c.cupos}
                  </TableCell>
                  <TableCell>
                    <EstadoCampaniaBadge estado={c.estado} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
