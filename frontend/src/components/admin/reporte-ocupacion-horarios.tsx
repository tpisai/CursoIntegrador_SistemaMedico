"use client"

import { useState } from "react"
import { CalendarClockIcon, CircleAlertIcon, DownloadIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useReporteOcupacionHorarios } from "@/hooks/use-admin"
import { mensajeDeError } from "@/lib/api/http"
import type { FilaOcupacion, ReporteOcupacionHorarios as ReporteOcupacion } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { toFechaISO } from "@/lib/format"

const COLUMNAS = ["Turnos ofertados", "Ocupados", "Disponibles", "Cancelados", "% Ocupación"]

function valores(fila: FilaOcupacion) {
  return [fila.ofertados, fila.ocupados, fila.disponibles, fila.cancelados, `${fila.porcentajeOcupacion} %`]
}

function TablaOcupacion({
  titulo,
  columna,
  filas,
  totales,
}: {
  titulo: string
  columna: string
  filas: FilaOcupacion[]
  totales: FilaOcupacion
}) {
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

function descargarCsv(reporte: ReporteOcupacion) {
  const texto = (valor: string | number) => `"${String(valor).replaceAll('"', '""')}"`
  const seccion = (titulo: string, filas: FilaOcupacion[]) => [
    [titulo, ...COLUMNAS],
    ...filas.map((fila) => [fila.nombre, ...valores(fila)]),
    ["Total", ...valores(reporte.totales)],
    [],
  ]
  const lineas = [...seccion("Especialidad", reporte.porEspecialidad), ...seccion("Doctor", reporte.porDoctor)]
  const csv = lineas.map((linea) => linea.map(texto).join(";")).join("\r\n")
  guardarArchivo(
    new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8" }),
    `ocupacion-horarios-${reporte.desde}-a-${reporte.hasta}.csv`
  )
}

export function ReporteOcupacionHorarios() {
  const [desde, setDesde] = useState(() => {
    const hoy = new Date()
    return toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  })
  const [hasta, setHasta] = useState(() => {
    const futuro = new Date()
    futuro.setDate(futuro.getDate() + 15)
    return toFechaISO(futuro)
  })
  const rangoValido = Boolean(desde && hasta && desde <= hasta)
  const { data: reporte, error, isLoading } = useReporteOcupacionHorarios(
    rangoValido ? desde : null,
    rangoValido ? hasta : null
  )

  return (
    <div className="mt-8 flex flex-col gap-6 border-t pt-8">
      <Card>
        <CardHeader>
          <CardTitle>Capacidad y ocupación de horarios ofertados</CardTitle>
          <CardDescription>
            Analiza los turnos programados en agenda, disponibilidad libre y porcentaje de aprovechamiento por especialidad y doctor.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap items-start justify-between gap-4">
          <div className="grid w-full gap-4 sm:w-auto sm:grid-cols-2">
            <Field>
              <FieldLabel htmlFor="ocupacion-desde">Desde</FieldLabel>
              <Input id="ocupacion-desde" type="date" className="h-11" value={desde} onChange={(e) => setDesde(e.target.value)} />
            </Field>
            <Field data-invalid={!rangoValido}>
              <FieldLabel htmlFor="ocupacion-hasta">Hasta</FieldLabel>
              <Input
                id="ocupacion-hasta"
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
            disabled={!reporte?.totales.ofertados}
            onClick={() => reporte && descargarCsv(reporte)}
          >
            <DownloadIcon data-icon="inline-start" />
            Descargar ocupación Excel
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
      ) : reporte && reporte.totales.ofertados === 0 ? (
        <Empty className="border border-dashed p-8">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <CalendarClockIcon />
            </EmptyMedia>
            <EmptyTitle>Sin horarios ofertados en este periodo</EmptyTitle>
            <EmptyDescription>Amplía el rango de fechas para evaluar la disponibilidad de turnos.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : reporte ? (
        <>
          <TablaOcupacion
            titulo="Ocupación de horarios por especialidad"
            columna="Especialidad"
            filas={reporte.porEspecialidad}
            totales={reporte.totales}
          />
          <TablaOcupacion
            titulo="Ocupación de horarios por doctor"
            columna="Doctor"
            filas={reporte.porDoctor}
            totales={reporte.totales}
          />
        </>
      ) : null}
    </div>
  )
}