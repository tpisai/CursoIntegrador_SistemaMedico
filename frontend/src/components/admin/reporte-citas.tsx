"use client"

import { useMemo, useState } from "react"
import { CalendarSearchIcon, CircleAlertIcon, DownloadIcon, FileTextIcon, RotateCcwIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useFiltrosReporte, useReporteCitas } from "@/hooks/use-admin"
import { mensajeDeError } from "@/lib/api/http"
import type { FilaReporte, ReporteCitas as Reporte } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { toFechaISO } from "@/lib/format"

const COLUMNAS = ["Citas", "Atendidas", "No asistió", "Canceladas", "Pendientes", "Ausentismo"]
const ESTADOS = [
  { value: "", label: "Todos los estados" },
  { value: "RESERVADA", label: "Reservada" },
  { value: "CONFIRMADA", label: "Confirmada" },
  { value: "ATENDIDA", label: "Atendida" },
  { value: "NO_ASISTIO", label: "No asistió" },
  { value: "CANCELADA", label: "Cancelada" },
]

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
                <TableHead key={c} className="text-right">{c}</TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {filas.map((fila) => (
              <TableRow key={`${fila.idEspecialidad ?? "e"}-${fila.idDoctor ?? "d"}-${fila.nombre}`}>
                <TableCell>
                  <span className="font-medium">{fila.nombre}</span>
                  {fila.detalle ? <span className="block text-muted-foreground">{fila.detalle}</span> : null}
                </TableCell>
                {valores(fila).map((valor, i) => (
                  <TableCell key={COLUMNAS[i]} className="text-right tabular-nums">{valor}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell>Total</TableCell>
              {valores(totales).map((valor, i) => (
                <TableCell key={COLUMNAS[i]} className="text-right tabular-nums">{valor}</TableCell>
              ))}
            </TableRow>
          </TableFooter>
        </Table>
      </CardContent>
    </Card>
  )
}

function GraficoBarras({ titulo, filas }: { titulo: string; filas: FilaReporte[] }) {
  const maximo = Math.max(1, ...filas.map((fila) => fila.total))
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{titulo}</CardTitle>
        <CardDescription>Distribución de citas según los filtros aplicados.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {filas.length ? filas.map((fila) => (
          <div key={`${fila.idEspecialidad ?? "e"}-${fila.idDoctor ?? "d"}-${fila.nombre}`} className="space-y-1">
            <div className="flex justify-between gap-3 text-sm">
              <span className="truncate font-medium">{fila.nombre}</span>
              <span className="shrink-0 tabular-nums text-muted-foreground">{fila.total}</span>
            </div>
            <div className="h-2.5 overflow-hidden rounded-full bg-muted">
              <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${(fila.total / maximo) * 100}%` }} />
            </div>
          </div>
        )) : <p className="text-sm text-muted-foreground">No hay datos para mostrar.</p>}
      </CardContent>
    </Card>
  )
}

function descargarCsv(reporte: Reporte, criterios: string[]) {
  const texto = (valor: string | number) => `"${String(valor).replaceAll('"', '""')}"`
  const seccion = (titulo: string, filas: FilaReporte[]) => [
    [titulo, ...COLUMNAS],
    ...filas.map((fila) => [fila.nombre, ...valores(fila)]),
    ["Total", ...valores(reporte.totales)],
    [],
  ]
  const lineas = [
    ["Periodo", `${reporte.desde} al ${reporte.hasta}`],
    ["Filtros", criterios.length ? criterios.join(" | ") : "Sin filtros adicionales"],
    [],
    ...seccion("Especialidad", reporte.porEspecialidad),
    ...seccion("Doctor", reporte.porDoctor),
  ]
  const csv = lineas.map((linea) => linea.map(texto).join(";")).join("\r\n")
  guardarArchivo(new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8" }), `reporte-citas-${reporte.desde}-a-${reporte.hasta}.csv`)
}

function escaparHtml(valor: string) {
  return valor.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;")
}

function descargarPdf(reporte: Reporte, criterios: string[]) {
  const filas = (datos: FilaReporte[]) => datos.map((fila) => `
    <tr><td>${escaparHtml(fila.nombre)}${fila.detalle ? `<small>${escaparHtml(fila.detalle)}</small>` : ""}</td>
    <td>${fila.total}</td><td>${fila.atendidas}</td><td>${fila.noAsistio}</td><td>${fila.canceladas}</td><td>${fila.pendientes}</td><td>${fila.ausentismo} %</td></tr>
  `).join("")
  const tabla = (titulo: string, datos: FilaReporte[]) => `
    <h2>${titulo}</h2><table><thead><tr><th>Nombre</th>${COLUMNAS.map((col) => `<th>${col}</th>`).join("")}</tr></thead>
    <tbody>${filas(datos)}<tr class="total"><td>Total</td><td>${reporte.totales.total}</td><td>${reporte.totales.atendidas}</td>
    <td>${reporte.totales.noAsistio}</td><td>${reporte.totales.canceladas}</td><td>${reporte.totales.pendientes}</td><td>${reporte.totales.ausentismo} %</td></tr></tbody></table>
  `
  const html = `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Reporte de citas</title>
  <style>body{font:12px Arial,sans-serif;color:#172033;padding:24px}h1{color:#155e75;font-size:21px}h2{font-size:15px;margin-top:24px}
  p{color:#475569}table{width:100%;border-collapse:collapse;margin-top:8px}th{background:#155e75;color:white;text-align:left}
  th,td{padding:7px;border:1px solid #dbe2ea;text-align:right}th:first-child,td:first-child{text-align:left}
  small{display:block;color:#64748b}.total{font-weight:bold;background:#f1f5f9}@media print{body{padding:0}}</style></head>
  <body><h1>Reporte administrativo de citas</h1><p>Periodo: ${escaparHtml(reporte.desde)} al ${escaparHtml(reporte.hasta)}</p>
  <p>Filtros: ${escaparHtml(criterios.length ? criterios.join(" · ") : "Sin filtros adicionales")}</p>
  <p>Total: ${reporte.totales.total} · Atendidas: ${reporte.totales.atendidas} · No asistió: ${reporte.totales.noAsistio} · Canceladas: ${reporte.totales.canceladas} · Pendientes: ${reporte.totales.pendientes}</p>
  ${tabla("Citas por especialidad", reporte.porEspecialidad)}${tabla("Citas por doctor", reporte.porDoctor)}
  <script>window.onload=()=>window.print()</script></body></html>`
  const ventana = window.open("", "_blank", "width=1100,height=800")
  if (!ventana) return
  ventana.document.write(html)
  ventana.document.close()
}

// EDT 4.8: reporte administrativo con filtros aplicados en el repositorio JPA.
export function ReporteCitas() {
  const [desde, setDesde] = useState(() => {
    const hoy = new Date()
    return toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  })
  const [hasta, setHasta] = useState(() => toFechaISO(new Date()))
  const [idEspecialidad, setIdEspecialidad] = useState("")
  const [idDoctor, setIdDoctor] = useState("")
  const [estado, setEstado] = useState("")
  const rangoValido = Boolean(desde && hasta && desde <= hasta)
  const { data: filtros, error: errorFiltros } = useFiltrosReporte()
  const { data: reporte, error, isLoading } = useReporteCitas(
    rangoValido ? desde : null,
    rangoValido ? hasta : null,
    idEspecialidad ? Number(idEspecialidad) : undefined,
    idDoctor ? Number(idDoctor) : undefined,
    estado || undefined,
  )
  const doctores = useMemo(
    () => (filtros?.doctores ?? []).filter((doctor) => !idEspecialidad || filtros?.especialidades.some(
      (especialidad) => especialidad.id === Number(idEspecialidad) && especialidad.nombre === doctor.detalle,
    )),
    [filtros, idEspecialidad],
  )
  const criterios = [
    idEspecialidad ? `Especialidad: ${filtros?.especialidades.find((item) => item.id === Number(idEspecialidad))?.nombre ?? idEspecialidad}` : "",
    idDoctor ? `Doctor: ${filtros?.doctores.find((item) => item.id === Number(idDoctor))?.nombre ?? idDoctor}` : "",
    estado ? `Estado: ${ESTADOS.find((item) => item.value === estado)?.label ?? estado}` : "",
  ].filter(Boolean)

  function limpiarFiltros() {
    setIdEspecialidad("")
    setIdDoctor("")
    setEstado("")
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Filtros del reporte de citas</CardTitle>
          <CardDescription>Los indicadores, gráficos y archivos exportados se actualizan con los filtros seleccionados.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field>
            <FieldLabel htmlFor="reporte-desde">Desde</FieldLabel>
            <Input id="reporte-desde" type="date" className="h-11" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </Field>
          <Field data-invalid={!rangoValido}>
            <FieldLabel htmlFor="reporte-hasta">Hasta</FieldLabel>
            <Input id="reporte-hasta" type="date" className="h-11" aria-invalid={!rangoValido} value={hasta} onChange={(e) => setHasta(e.target.value)} />
            {rangoValido ? null : <FieldError>Debe ser igual o posterior a la fecha inicial.</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="reporte-especialidad">Especialidad</FieldLabel>
            <select id="reporte-especialidad" className="h-11 w-full rounded-md border bg-background px-3 text-sm" value={idEspecialidad}
              onChange={(e) => { setIdEspecialidad(e.target.value); setIdDoctor("") }}>
              <option value="">Todas las especialidades</option>
              {(filtros?.especialidades ?? []).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
            </select>
          </Field>
          <Field>
            <FieldLabel htmlFor="reporte-doctor">Doctor</FieldLabel>
            <select id="reporte-doctor" className="h-11 w-full rounded-md border bg-background px-3 text-sm" value={idDoctor} onChange={(e) => setIdDoctor(e.target.value)}>
              <option value="">Todos los doctores</option>
              {doctores.map((item) => <option key={item.id} value={item.id}>{item.nombre} — {item.detalle}</option>)}
            </select>
          </Field>
          <Field>
            <FieldLabel htmlFor="reporte-estado">Estado de cita</FieldLabel>
            <select id="reporte-estado" className="h-11 w-full rounded-md border bg-background px-3 text-sm" value={estado} onChange={(e) => setEstado(e.target.value)}>
              {ESTADOS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </Field>
          <div className="flex items-end">
            <Button variant="outline" className="h-11 w-full" onClick={limpiarFiltros}>
              <RotateCcwIcon data-icon="inline-start" /> Limpiar filtros
            </Button>
          </div>
          <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-3">
            <Button variant="outline" className="h-11" disabled={!reporte?.totales.total} onClick={() => reporte && descargarCsv(reporte, criterios)}>
              <DownloadIcon data-icon="inline-start" /> Descargar Excel (CSV)
            </Button>
            <Button className="h-11" disabled={!reporte?.totales.total} onClick={() => reporte && descargarPdf(reporte, criterios)}>
              <FileTextIcon data-icon="inline-start" /> Descargar PDF
            </Button>
          </div>
        </CardContent>
      </Card>

      {errorFiltros ? <Alert variant="destructive"><AlertDescription>{mensajeDeError(errorFiltros)}</AlertDescription></Alert> : null}
      {isLoading ? <Skeleton className="h-64 rounded-xl" /> : error ? (
        <Alert variant="destructive"><CircleAlertIcon /><AlertDescription>{mensajeDeError(error)}</AlertDescription></Alert>
      ) : reporte && reporte.totales.total === 0 ? (
        <Empty className="border border-dashed p-8">
          <EmptyHeader>
            <EmptyMedia variant="icon"><CalendarSearchIcon /></EmptyMedia>
            <EmptyTitle>Sin citas con los filtros seleccionados</EmptyTitle>
            <EmptyDescription>Prueba con otro periodo o elimina alguno de los filtros.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : reporte ? (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
            {[
              ["Total de citas", reporte.totales.total],
              ["Atendidas", reporte.totales.atendidas],
              ["No asistió", reporte.totales.noAsistio],
              ["Canceladas", reporte.totales.canceladas],
              ["Pendientes", reporte.totales.pendientes],
              ["Ausentismo", `${reporte.totales.ausentismo} %`],
            ].map(([titulo, valor]) => (
              <Card key={titulo}><CardContent className="p-4">
                <p className="text-sm text-muted-foreground">{titulo}</p>
                <p className="mt-1 text-2xl font-semibold tabular-nums">{valor}</p>
              </CardContent></Card>
            ))}
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <GraficoBarras titulo="Citas por especialidad" filas={reporte.porEspecialidad} />
            <GraficoBarras titulo="Citas por doctor" filas={reporte.porDoctor} />
          </div>
          <TablaReporte titulo="Detalle de citas por especialidad" columna="Especialidad" filas={reporte.porEspecialidad} totales={reporte.totales} />
          <TablaReporte titulo="Detalle de citas por doctor" columna="Doctor" filas={reporte.porDoctor} totales={reporte.totales} />
        </>
      ) : null}
    </>
  )
}

