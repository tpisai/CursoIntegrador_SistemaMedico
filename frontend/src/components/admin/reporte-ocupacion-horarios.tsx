"use client"

import { useState } from "react"
import { BarChart3Icon, CalendarClockIcon, DownloadIcon, FileTextIcon, CircleAlertIcon } from "lucide-react"

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

function escapeHtml(texto: string | number) {
  return String(texto)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
}

function colorBarra(porcentaje: number) {
  if (porcentaje >= 70) return "bg-emerald-600"
  if (porcentaje >= 35) return "bg-sky-600"
  return "bg-teal-600"
}

function GraficoBarras({
  titulo,
  subtitulo,
  filas,
}: {
  titulo: string
  subtitulo: string
  filas: FilaOcupacion[]
}) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <BarChart3Icon className="size-4 text-primary" />
          {titulo}
        </CardTitle>
        <CardDescription>{subtitulo}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {filas.map((fila) => {
          const pct = Math.min(100, Math.max(0, fila.porcentajeOcupacion))
          return (
            <div key={fila.nombre} className="space-y-1.5">
              <div className="flex items-baseline justify-between gap-2 text-sm">
                <div className="min-w-0">
                  <span className="font-medium">{fila.nombre}</span>
                  {fila.detalle ? (
                    <span className="ml-2 text-xs text-muted-foreground">({fila.detalle})</span>
                  ) : null}
                </div>
                <span className="shrink-0 tabular-nums text-xs font-medium text-muted-foreground">
                  {fila.ocupados}/{fila.ofertados} turnos ({fila.porcentajeOcupacion} %)
                </span>
              </div>
              <div className="h-3 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${colorBarra(pct)}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          )
        })}
      </CardContent>
    </Card>
  )
}

function ResumenGraficoGeneral({ totales }: { totales: FilaOcupacion }) {
  const total = Math.max(1, totales.ofertados)
  const pctOcupados = Math.round((totales.ocupados / total) * 100)
  const pctDisponibles = Math.round((totales.disponibles / total) * 100)
  const pctCancelados = Math.max(0, 100 - pctOcupados - pctDisponibles)

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">Distribución general de turnos ofertados</CardTitle>
        <CardDescription>
          Proporción entre turnos reservados/atendidos, disponibles en agenda y cancelados en el periodo seleccionado.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          <div className="rounded-lg border bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">Ofertados</p>
            <p className="mt-1 text-xl font-semibold tabular-nums">{totales.ofertados}</p>
          </div>
          <div className="rounded-lg border bg-teal-50/60 p-3 dark:bg-teal-950/20">
            <p className="text-xs text-teal-700 dark:text-teal-300">Ocupados</p>
            <p className="mt-1 text-xl font-semibold tabular-nums text-teal-800 dark:text-teal-200">{totales.ocupados}</p>
          </div>
          <div className="rounded-lg border bg-sky-50/60 p-3 dark:bg-sky-950/20">
            <p className="text-xs text-sky-700 dark:text-sky-300">Disponibles</p>
            <p className="mt-1 text-xl font-semibold tabular-nums text-sky-800 dark:text-sky-200">{totales.disponibles}</p>
          </div>
          <div className="rounded-lg border bg-rose-50/60 p-3 dark:bg-rose-950/20">
            <p className="text-xs text-rose-700 dark:text-rose-300">Cancelados</p>
            <p className="mt-1 text-xl font-semibold tabular-nums text-rose-800 dark:text-rose-200">{totales.cancelados}</p>
          </div>
          <div className="col-span-2 rounded-lg border bg-emerald-50/60 p-3 sm:col-span-1 dark:bg-emerald-950/20">
            <p className="text-xs text-emerald-700 dark:text-emerald-300">% Ocupación</p>
            <p className="mt-1 text-xl font-semibold tabular-nums text-emerald-800 dark:text-emerald-200">
              {totales.porcentajeOcupacion} %
            </p>
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex h-4 w-full overflow-hidden rounded-full bg-muted">
            {totales.ocupados > 0 ? (
              <div
                className="bg-teal-600 transition-all"
                style={{ width: `${Math.max(2, pctOcupados)}%` }}
                title={`Ocupados: ${totales.ocupados}`}
              />
            ) : null}
            {totales.disponibles > 0 ? (
              <div
                className="bg-sky-400 transition-all"
                style={{ width: `${Math.max(2, pctDisponibles)}%` }}
                title={`Disponibles: ${totales.disponibles}`}
              />
            ) : null}
            {totales.cancelados > 0 ? (
              <div
                className="bg-rose-500 transition-all"
                style={{ width: `${Math.max(2, pctCancelados)}%` }}
                title={`Cancelados: ${totales.cancelados}`}
              />
            ) : null}
          </div>
          <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="size-2.5 rounded-full bg-teal-600" />
              Ocupados ({totales.ocupados})
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-2.5 rounded-full bg-sky-400" />
              Disponibles ({totales.disponibles})
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-2.5 rounded-full bg-rose-500" />
              Cancelados ({totales.cancelados})
            </span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
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

function exportarPdf(reporte: ReporteOcupacion) {
  const ventana = window.open("", "_blank", "width=1024,height=768")
  if (!ventana) return

  const fechaGeneracion = new Date().toLocaleString("es-PE", {
    dateStyle: "medium",
    timeStyle: "short",
  })

  const renderBarras = (titulo: string, filas: FilaOcupacion[]) => `
    <div class="card">
      <h3>${escapeHtml(titulo)}</h3>
      ${filas
        .map((f) => {
          const pct = Math.min(100, Math.max(0, f.porcentajeOcupacion))
          return `
            <div class="bar-item">
              <div class="bar-label">
                <span><strong>${escapeHtml(f.nombre)}</strong>${f.detalle ? ` <small>(${escapeHtml(f.detalle)})</small>` : ""}</span>
                <span>${f.ocupados}/${f.ofertados} turnos (${f.porcentajeOcupacion} %)</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" style="width: ${pct}%;"></div>
              </div>
            </div>
          `
        })
        .join("")}
    </div>
  `

  const renderTabla = (titulo: string, columna: string, filas: FilaOcupacion[]) => `
    <div class="card">
      <h3>${escapeHtml(titulo)}</h3>
      <table>
        <thead>
          <tr>
            <th>${escapeHtml(columna)}</th>
            <th class="num">Ofertados</th>
            <th class="num">Ocupados</th>
            <th class="num">Disponibles</th>
            <th class="num">Cancelados</th>
            <th class="num">% Ocupación</th>
          </tr>
        </thead>
        <tbody>
          ${filas
            .map(
              (f) => `
            <tr>
              <td>
                <strong>${escapeHtml(f.nombre)}</strong>
                ${f.detalle ? `<div class="sub">${escapeHtml(f.detalle)}</div>` : ""}
              </td>
              <td class="num">${f.ofertados}</td>
              <td class="num">${f.ocupados}</td>
              <td class="num">${f.disponibles}</td>
              <td class="num">${f.cancelados}</td>
              <td class="num">${f.porcentajeOcupacion} %</td>
            </tr>
          `
            )
            .join("")}
        </tbody>
        <tfoot>
          <tr>
            <td><strong>Total</strong></td>
            <td class="num"><strong>${reporte.totales.ofertados}</strong></td>
            <td class="num"><strong>${reporte.totales.ocupados}</strong></td>
            <td class="num"><strong>${reporte.totales.disponibles}</strong></td>
            <td class="num"><strong>${reporte.totales.cancelados}</strong></td>
            <td class="num"><strong>${reporte.totales.porcentajeOcupacion} %</strong></td>
          </tr>
        </tfoot>
      </table>
    </div>
  `

  const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>Reporte de Ocupación de Horarios (${escapeHtml(reporte.desde)} al ${escapeHtml(reporte.hasta)})</title>
  <style>
    * { box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      color: #0f172a;
      margin: 0;
      padding: 24px;
      background: #ffffff;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    .header {
      border-bottom: 2px solid #0d9488;
      padding-bottom: 14px;
      margin-bottom: 18px;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }
    .header h1 { margin: 0 0 4px; font-size: 20px; color: #0f766e; }
    .header p { margin: 0; font-size: 12px; color: #475569; }
    .meta { text-align: right; font-size: 11px; color: #475569; }
    .kpis {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 10px;
      margin-bottom: 18px;
    }
    .kpi {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 10px;
      background: #f8fafc;
    }
    .kpi span { display: block; font-size: 11px; color: #64748b; }
    .kpi strong { display: block; font-size: 18px; margin-top: 2px; color: #0f172a; }
    .grid-charts {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-bottom: 18px;
    }
    .card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 14px;
      margin-bottom: 14px;
      break-inside: avoid;
    }
    .card h3 { margin: 0 0 12px; font-size: 14px; color: #0f172a; }
    .bar-item { margin-bottom: 10px; }
    .bar-label {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      margin-bottom: 4px;
    }
    .bar-label small { color: #64748b; }
    .bar-track {
      height: 10px;
      background: #e2e8f0;
      border-radius: 999px;
      overflow: hidden;
    }
    .bar-fill {
      height: 100%;
      background: #0d9488;
      border-radius: 999px;
    }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th, td { border-bottom: 1px solid #e2e8f0; padding: 7px 8px; text-align: left; }
    th { background: #f1f5f9; font-weight: 600; color: #334155; }
    tfoot td { background: #f8fafc; border-top: 2px solid #cbd5e1; }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .sub { font-size: 11px; color: #64748b; }
    @media print {
      body { padding: 0; }
    }
  </style>
</head>
<body>
  <div class="header">
    <div>
      <h1>SaludGrau · Centro de Salud Miguel Grau (Chaclacayo)</h1>
      <p>Reporte de capacidad y ocupación de horarios ofertados por especialidad y doctor</p>
    </div>
    <div class="meta">
      <div><strong>Periodo:</strong> ${escapeHtml(reporte.desde)} al ${escapeHtml(reporte.hasta)}</div>
      <div><strong>Generado:</strong> ${escapeHtml(fechaGeneracion)}</div>
    </div>
  </div>

  <div class="kpis">
    <div class="kpi"><span>Turnos ofertados</span><strong>${reporte.totales.ofertados}</strong></div>
    <div class="kpi"><span>Ocupados</span><strong>${reporte.totales.ocupados}</strong></div>
    <div class="kpi"><span>Disponibles</span><strong>${reporte.totales.disponibles}</strong></div>
    <div class="kpi"><span>Cancelados</span><strong>${reporte.totales.cancelados}</strong></div>
    <div class="kpi"><span>% Ocupación</span><strong>${reporte.totales.porcentajeOcupacion} %</strong></div>
  </div>

  <div class="grid-charts">
    ${renderBarras("Gráfico: Ocupación por especialidad (%)", reporte.porEspecialidad)}
    ${renderBarras("Gráfico: Ocupación por doctor (%)", reporte.porDoctor)}
  </div>

  ${renderTabla("Detalle de ocupación por especialidad", "Especialidad", reporte.porEspecialidad)}
  ${renderTabla("Detalle de ocupación por doctor", "Doctor", reporte.porDoctor)}

  <script>
    window.onload = () => {
      window.focus();
      window.print();
    };
  </script>
</body>
</html>`

  ventana.document.open()
  ventana.document.write(html)
  ventana.document.close()
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
          <div className="flex flex-wrap gap-3 self-end">
            <Button
              variant="outline"
              className="h-11"
              disabled={!reporte?.totales.ofertados}
              onClick={() => reporte && descargarCsv(reporte)}
            >
              <DownloadIcon data-icon="inline-start" />
              Exportar Excel
            </Button>
            <Button
              className="h-11"
              disabled={!reporte?.totales.ofertados}
              onClick={() => reporte && exportarPdf(reporte)}
            >
              <FileTextIcon data-icon="inline-start" />
              Exportar PDF
            </Button>
          </div>
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
          <ResumenGraficoGeneral totales={reporte.totales} />

          <div className="grid gap-6 lg:grid-cols-2">
            <GraficoBarras
              titulo="Gráfico de ocupación por especialidad"
              subtitulo="Porcentaje de turnos ocupados respecto a la oferta total por área."
              filas={reporte.porEspecialidad}
            />
            <GraficoBarras
              titulo="Gráfico de ocupación por doctor"
              subtitulo="Aprovechamiento de la agenda individual de cada médico."
              filas={reporte.porDoctor}
            />
          </div>

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