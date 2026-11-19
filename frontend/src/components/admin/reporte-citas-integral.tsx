"use client"

import { useEffect, useState } from "react"
import { DownloadIcon, EyeIcon, FileTextIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { descargarReporteIntegralCitas } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import { guardarArchivo } from "@/lib/descargar"
import { toFechaISO } from "@/lib/format"

const SECCIONES = [
  { id: "RESUMEN", texto: "Resultados y eventos" },
  { id: "ESPECIALIDAD", texto: "Citas por especialidad" },
  { id: "DOCTORES", texto: "Doctores con más citas" },
  { id: "CAMBIOS", texto: "Cambios de fecha" },
  { id: "DERIVACIONES", texto: "Derivaciones hospitalarias" },
] as const

function diferenciaDias(desde: string, hasta: string) {
  const inicio = new Date(`${desde}T00:00:00`)
  const fin = new Date(`${hasta}T00:00:00`)
  return Math.round((fin.getTime() - inicio.getTime()) / 86_400_000)
}

export function ReporteCitasIntegral() {
  const [desde, setDesde] = useState(() => {
    const hoy = new Date()
    return toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  })
  const [hasta, setHasta] = useState(() => toFechaISO(new Date()))
  const [secciones, setSecciones] = useState<string[]>(SECCIONES.map((s) => s.id))
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null)
  const dias = desde && hasta ? diferenciaDias(desde, hasta) : -1
  const fechasValidas = dias >= 0 && dias <= 366
  const formularioValido = fechasValidas && secciones.length > 0

  useEffect(() => () => {
    if (vistaPrevia) URL.revokeObjectURL(vistaPrevia)
  }, [vistaPrevia])

  function invalidarVistaPrevia() {
    setVistaPrevia(null)
    setError(null)
  }

  function cambiarSeccion(id: string, marcada: boolean) {
    setSecciones((actuales) => marcada ? [...actuales, id] : actuales.filter((actual) => actual !== id))
    invalidarVistaPrevia()
  }

  async function generarPdf() {
    if (!formularioValido) return null
    setCargando(true)
    setError(null)
    try {
      return await descargarReporteIntegralCitas(desde, hasta, secciones)
    } catch (e) {
      setError(mensajeDeError(e))
      return null
    } finally {
      setCargando(false)
    }
  }

  async function previsualizar() {
    const pdf = await generarPdf()
    if (pdf) setVistaPrevia(URL.createObjectURL(pdf))
  }

  async function descargar() {
    const pdf = await generarPdf()
    if (pdf) guardarArchivo(pdf, `reporte-integral-citas-${desde}-a-${hasta}.pdf`)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reporte integral de citas (PDF)</CardTitle>
        <CardDescription>
          Configura el periodo y el contenido. Podés revisar el PDF antes de descargarlo; las métricas de eventos pueden superponerse.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="reporte-integral-desde">Desde</FieldLabel>
            <Input id="reporte-integral-desde" type="date" value={desde} onChange={(e) => { setDesde(e.target.value); invalidarVistaPrevia() }} />
          </Field>
          <Field data-invalid={!fechasValidas}>
            <FieldLabel htmlFor="reporte-integral-hasta">Hasta</FieldLabel>
            <Input
              id="reporte-integral-hasta"
              type="date"
              aria-invalid={!fechasValidas}
              value={hasta}
              onChange={(e) => { setHasta(e.target.value); invalidarVistaPrevia() }}
            />
            {fechasValidas ? null : (
              <FieldError>{dias < 0 ? "La fecha final debe ser igual o posterior a la inicial." : "El periodo máximo es de un año."}</FieldError>
            )}
          </Field>
        </div>

        <fieldset className="space-y-3 rounded-lg border p-4">
          <legend className="px-1 text-sm font-medium">Filtrar las consultas incluidas</legend>
          <p className="text-sm text-muted-foreground">Elegí qué secciones querés incorporar. Dejá todas seleccionadas para el reporte completo.</p>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {SECCIONES.map((seccion) => (
              <label key={seccion.id} className="flex min-h-10 cursor-pointer items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="size-4 accent-primary"
                  checked={secciones.includes(seccion.id)}
                  onChange={(e) => cambiarSeccion(seccion.id, e.target.checked)}
                />
                {seccion.texto}
              </label>
            ))}
          </div>
          {secciones.length ? null : <p className="text-sm text-destructive">Seleccioná al menos una consulta.</p>}
        </fieldset>

        {error ? <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert> : null}
        <div className="flex flex-wrap gap-3">
          <Button variant="outline" className="h-11" disabled={!formularioValido || cargando} onClick={previsualizar}>
            {cargando ? <FileTextIcon data-icon="inline-start" /> : <EyeIcon data-icon="inline-start" />}
            {cargando ? "Generando vista previa…" : "Previsualizar PDF"}
          </Button>
          <Button className="h-11" disabled={!formularioValido || cargando} onClick={descargar}>
            <DownloadIcon data-icon="inline-start" />
            Descargar PDF
          </Button>
        </div>

        {vistaPrevia ? (
          <section aria-label="Vista previa del reporte integral" className="space-y-2">
            <h3 className="text-sm font-semibold">Vista previa — {desde} al {hasta}</h3>
            <iframe
              title="Vista previa del reporte integral de citas"
              src={vistaPrevia}
              className="h-[min(75vh,900px)] w-full rounded-lg border bg-background"
            />
          </section>
        ) : null}
      </CardContent>
    </Card>
  )
}
