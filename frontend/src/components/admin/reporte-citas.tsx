"use client"

import { useState } from "react"
import { CalendarSearchIcon, CircleAlertIcon, DownloadIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useReporteCitas } from "@/hooks/use-admin"
import { mensajeDeError } from "@/lib/api/http"
import type { FilaReporte, ReporteCitas as Reporte } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { toFechaISO } from "@/lib/format"

const COLUMNAS = ["Citas", "Atendidas", "No asistió", "Canceladas", "Pendientes", "Ausentismo"]

function valores(fila: FilaReporte) {
  return [fila.total, fila.atendidas, fila.noAsistio, fila.canceladas, fila.pendientes, `${fila.ausentismo} %`]
}

function TablaReporte({ titulo, columna, filas, totales }: { titulo: string; columna: string; filas: FilaReporte[]; totales: FilaReporte }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{titulo}</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{columna}</TableHead>
              {COLUMNAS.map((c) => (
                <TableHead key={c} className="text-right">
                  {c}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {filas.map((fila) => (
              <TableRow key={fila.nombre}>
                <TableCell>
                  <span className="font-medium">{fila.nombre}</span>
                  {fila.detalle ? <span className="block text-muted-foreground">{fila.detalle}</span> : null}
                </TableCell>
                {valores(fila).map((valor, i) => (
                  <TableCell key={COLUMNAS[i]} className="text-right tabular-nums">
                    {valor}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell>Total</TableCell>
              {valores(totales).map((valor, i) => (
                <TableCell key={COLUMNAS[i]} className="text-right tabular-nums">
                  {valor}
                </TableCell>
              ))}
            </TableRow>
          </TableFooter>
        </Table>
      </CardContent>
    </Card>
  )
}

function descargarCsv(reporte: Reporte) {
  const texto = (valor: string | number) => `"${String(valor).replaceAll('"', '""')}"`
  const seccion = (titulo: string, filas: FilaReporte[]) => [
    [titulo, ...COLUMNAS],
    ...filas.map((fila) => [fila.nombre, ...valores(fila)]),
    ["Total", ...valores(reporte.totales)],
    [],
  ]
  const lineas = [...seccion("Especialidad", reporte.porEspecialidad), ...seccion("Doctor", reporte.porDoctor)]
  const csv = lineas.map((linea) => linea.map(texto).join(";")).join("\r\n")
  // La marca BOM hace que Excel lea bien las tildes.
  guardarArchivo(new Blob(["﻿", csv], { type: "text/csv;charset=utf-8" }), `reporte-citas-${reporte.desde}-a-${reporte.hasta}.csv`)
}

// EDT 4.8: reporte de control de citas y ausentismo.
export function ReporteCitas() {
  const [desde, setDesde] = useState(() => {
    const hoy = new Date()
    return toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  })
  const [hasta, setHasta] = useState(() => toFechaISO(new Date()))
  const rangoValido = Boolean(desde && hasta && desde <= hasta)
  const { data: reporte, error, isLoading } = useReporteCitas(rangoValido ? desde : null, rangoValido ? hasta : null)

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Periodo del reporte</CardTitle>
          <CardDescription>El ausentismo es el porcentaje de citas ya cerradas a las que el paciente no asistió.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap items-start justify-between gap-4">
          <div className="grid w-full gap-4 sm:w-auto sm:grid-cols-2">
            <Field>
              <FieldLabel htmlFor="reporte-desde">Desde</FieldLabel>
              <Input id="reporte-desde" type="date" className="h-11" value={desde} onChange={(e) => setDesde(e.target.value)} />
            </Field>
            <Field data-invalid={!rangoValido}>
              <FieldLabel htmlFor="reporte-hasta">Hasta</FieldLabel>
              <Input
                id="reporte-hasta"
                type="date"
                className="h-11"
                aria-invalid={!rangoValido}
                value={hasta}
                onChange={(e) => setHasta(e.target.value)}
              />
              {rangoValido ? null : <FieldError>Debe ser igual o posterior a la fecha inicial.</FieldError>}
            </Field>
          </div>
          <Button
            variant="outline"
            className="h-11 self-end"
            disabled={!reporte?.totales.total}
            onClick={() => reporte && descargarCsv(reporte)}
          >
            <DownloadIcon data-icon="inline-start" />
            Descargar para Excel
          </Button>
        </CardContent>
      </Card>

      {isLoading ? (
        <Skeleton className="h-64 rounded-xl" />
      ) : error ? (
        <Alert variant="destructive">
          <CircleAlertIcon />
          <AlertDescription>{mensajeDeError(error)}</AlertDescription>
        </Alert>
      ) : reporte && reporte.totales.total === 0 ? (
        <Empty className="border border-dashed p-8">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <CalendarSearchIcon />
            </EmptyMedia>
            <EmptyTitle>Sin citas en este periodo</EmptyTitle>
            <EmptyDescription>Elige otras fechas para ver el reporte.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : reporte ? (
        <>
          <TablaReporte titulo="Citas por especialidad" columna="Especialidad" filas={reporte.porEspecialidad} totales={reporte.totales} />
          <TablaReporte titulo="Citas por doctor" columna="Doctor" filas={reporte.porDoctor} totales={reporte.totales} />
        </>
      ) : null}
    </>
  )
}
