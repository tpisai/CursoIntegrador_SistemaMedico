"use client"

import { useState } from "react"
import { DownloadIcon, EyeIcon } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { consultarReporteIntegralCitas, descargarReporteIntegralCitas } from "@/lib/api/admin"
import { guardarArchivo } from "@/lib/descargar"
import { mensajeDeError } from "@/lib/api/http"
import type { ReporteIntegralCitas } from "@/lib/api/types"
import { toFechaISO } from "@/lib/format"
import { useFiltrosReporte } from "@/hooks/use-admin"

const ESTADOS = ["", "RESERVADA", "CONFIRMADA", "ATENDIDA", "NO_ASISTIO", "CANCELADA"]

export function ReporteCitasIntegral() {
  const hoy = new Date()
  const [desde, setDesde] = useState(toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1)))
  const [hasta, setHasta] = useState(toFechaISO(hoy))
  const [especialidad, setEspecialidad] = useState("")
  const [doctor, setDoctor] = useState("")
  const [estado, setEstado] = useState("")
  const [reporte, setReporte] = useState<ReporteIntegralCitas | null>(null)
  const { data: filtros, error: errorFiltros } = useFiltrosReporte()
  const doctores = (filtros?.doctores ?? []).filter(d => !especialidad || d.detalle === filtros?.especialidades.find(e => e.id === Number(especialidad))?.nombre)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const [pdfCargando, setPdfCargando] = useState(false)

  async function consultar() {
    setCargando(true); setError(null)
    try {
      setReporte(await consultarReporteIntegralCitas(desde, hasta, especialidad ? Number(especialidad) : undefined, doctor ? Number(doctor) : undefined, estado || undefined))
    } catch (e) { setError(mensajeDeError(e)) } finally { setCargando(false) }
  }


  async function generarPdf(previsualizar: boolean) {
    setPdfCargando(true); setError(null)
    try {
      const blob = await descargarReporteIntegralCitas(desde, hasta, [], especialidad ? Number(especialidad) : undefined, doctor ? Number(doctor) : undefined, estado || undefined)
      if (previsualizar) {
        if (pdfUrl) URL.revokeObjectURL(pdfUrl)
        setPdfUrl(URL.createObjectURL(blob))
      } else guardarArchivo(blob, `reporte-integral-citas-${desde}-a-${hasta}.pdf`)
    } catch (e) { setError(mensajeDeError(e)) } finally { setPdfCargando(false) }
  }

  return <div className="space-y-6">
    <Card><CardHeader><CardTitle>Reporte integral de citas</CardTitle></CardHeader><CardContent className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="text-sm">Desde<Input type="date" value={desde} onChange={e => setDesde(e.target.value)} /></label>
        <label className="text-sm">Hasta<Input type="date" value={hasta} onChange={e => setHasta(e.target.value)} /></label>
        <label className="text-sm">Especialidad<select className="mt-1 h-10 w-full rounded-md border bg-background px-3" value={especialidad} onChange={e => { setEspecialidad(e.target.value); setDoctor("") }}><option value="">Todas</option>{(filtros?.especialidades ?? []).map(op => <option key={op.id} value={op.id}>{op.nombre}</option>)}</select></label>
        <label className="text-sm">Doctor<select className="mt-1 h-10 w-full rounded-md border bg-background px-3" value={doctor} onChange={e => setDoctor(e.target.value)}><option value="">Todos</option>{doctores.map(op => <option key={op.id} value={op.id}>{op.nombre}</option>)}</select></label>
        <label className="text-sm">Estado<select className="mt-1 h-10 w-full rounded-md border bg-background px-3" value={estado} onChange={e => setEstado(e.target.value)}>{ESTADOS.map(e => <option key={e} value={e}>{e || "Todos"}</option>)}</select></label>
      </div>
      {errorFiltros && <Alert variant="destructive"><AlertDescription>{mensajeDeError(errorFiltros)}</AlertDescription></Alert>}
      <div className="flex flex-wrap gap-2">
        <Button onClick={consultar} disabled={cargando || !desde || !hasta}>{cargando ? "Consultando…" : "Aplicar filtros"}</Button>
        <Button variant="outline" onClick={() => generarPdf(true)} disabled={pdfCargando || !desde || !hasta}><EyeIcon data-icon="inline-start" />{pdfCargando ? "Generando…" : "Vista previa PDF"}</Button>
        <Button variant="outline" onClick={() => generarPdf(false)} disabled={pdfCargando || !desde || !hasta}><DownloadIcon data-icon="inline-start" />Descargar PDF</Button>
      </div>
      {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
      {pdfUrl && <iframe title="Vista previa del reporte integral" src={pdfUrl} className="h-[min(75vh,900px)] w-full rounded-lg border bg-background" />}
    </CardContent></Card>
    {reporte && <>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{[["Citas del periodo", reporte.totalProgramadas], ["Activas", reporte.activas], ["Atendidas", reporte.atendidas], ["No asistió", reporte.noAsistio], ["Canceladas", reporte.canceladas], ["Tasa de asistencia", `${reporte.tasaAsistencia}%`]].map(([titulo, valor]) => <Card key={String(titulo)}><CardContent className="p-5"><p className="text-sm text-muted-foreground">{titulo}</p><p className="text-2xl font-semibold">{valor}</p></CardContent></Card>)}</div>
      <Card><CardHeader><CardTitle>Evolución {reporte.granularidad.toLowerCase()}</CardTitle></CardHeader><CardContent className="w-full overflow-x-auto"><div className="flex h-64 min-w-full items-end gap-2 border-b border-l px-3 pb-8 pt-6" role="img" aria-label={`Histograma de citas por periodo, agrupación ${reporte.granularidad.toLowerCase()}`}>
        {reporte.evolucion.map(p => { const maximo = Math.max(1, ...reporte.evolucion.map(f => f.programadas)); const alto = p.programadas ? Math.max(4, p.programadas * 100 / maximo) : 0; return <div key={p.periodo} className="group relative flex h-full min-w-8 flex-1 flex-col justify-end" title={`${p.periodo}: ${p.programadas} citas`}><span className="mb-1 text-center text-xs tabular-nums">{p.programadas}</span><div className="w-full rounded-t bg-primary transition-colors group-hover:bg-primary/80" style={{ height: `${alto}%` }} /><span className="absolute -bottom-7 left-1/2 -translate-x-1/2 whitespace-nowrap text-[10px] text-muted-foreground">{p.periodo.slice(5)}</span></div> })}
      </div><p className="mt-3 text-xs text-muted-foreground">Cada barra representa una agrupación {reporte.granularidad.toLowerCase()} del periodo consultado.</p></CardContent></Card>
      <Card><CardHeader><CardTitle>Citas por especialidad y estado</CardTitle></CardHeader><CardContent className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr><th className="p-2">Especialidad</th><th className="p-2">Estado</th><th className="p-2">Citas</th></tr></thead><tbody>{reporte.porEspecialidad.map((f,i)=><tr key={`${f.especialidad}-${f.estado}-${i}`} className="border-t"><td className="p-2">{f.especialidad}</td><td className="p-2">{f.estado}</td><td className="p-2">{f.cantidad}</td></tr>)}</tbody></table></CardContent></Card>
      <Card><CardHeader><CardTitle>Detalle de citas (máximo 1.000 filas)</CardTitle></CardHeader><CardContent className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{["Fecha", "Hora", "Especialidad", "Doctor", "Estado"].map(x=><th key={x} className="p-2">{x}</th>)}</tr></thead><tbody>{reporte.detalle.map(f=><tr key={f.idCita} className="border-t"><td className="p-2">{f.fecha}</td><td className="p-2">{f.hora}</td><td className="p-2">{f.especialidad}</td><td className="p-2">{f.doctor}</td><td className="p-2">{f.estado}</td></tr>)}</tbody></table></CardContent></Card>
    </>}
  </div>
}
