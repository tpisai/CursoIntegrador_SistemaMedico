"use client"

import { useEffect, useState } from "react"
import { DownloadIcon, EyeIcon, FileTextIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldError, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useDoctores, useEspecialidades } from "@/hooks/use-citas"
import { useReportePrescripcion } from "@/hooks/use-admin"
import { descargarReportePrescripcionCsv, descargarReportePrescripcionPdf } from "@/lib/api/admin"
import { mensajeDeError } from "@/lib/api/http"
import type { TendenciaSemanal } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { toFechaISO } from "@/lib/format"

function diferenciaDias(desde: string, hasta: string) {
  const inicio = new Date(`${desde}T00:00:00`)
  const fin = new Date(`${hasta}T00:00:00`)
  return Math.round((fin.getTime() - inicio.getTime()) / 86_400_000)
}

function numero(valor: number, decimales = 0) {
  return new Intl.NumberFormat("es-PE", { maximumFractionDigits: decimales }).format(valor)
}

function BarrasHorizontales<T extends { nombre: string }>({
  filas,
  valor,
  tituloVacio,
}: {
  filas: T[]
  valor: (fila: T) => number
  tituloVacio: string
}) {
  const maximo = Math.max(1, ...filas.map(valor))
  if (!filas.length) return <p className="text-sm text-muted-foreground">{tituloVacio}</p>

  return (
    <ol className="space-y-3" aria-label={tituloVacio}>
      {filas.map((fila) => {
        const cantidad = valor(fila)
        return (
          <li key={fila.nombre} className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1">
            <span className="truncate text-sm" title={fila.nombre}>{fila.nombre}</span>
            <span className="text-sm font-semibold tabular-nums">{numero(cantidad)}</span>
            <div className="col-span-2 h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary"
                style={{ width: `${cantidad === 0 ? 0 : Math.max(2, 100 * cantidad / maximo)}%` }}
              />
            </div>
          </li>
        )
      })}
    </ol>
  )
}

function GraficoTendencia({ filas }: { filas: TendenciaSemanal[] }) {
  if (!filas.length) return <p className="text-sm text-muted-foreground">No hay recetas registradas en este periodo.</p>

  const maximo = Math.max(1, ...filas.map((fila) => fila.recetas))
  const x = (indice: number) => filas.length === 1 ? 320 : 32 + indice * 576 / (filas.length - 1)
  const y = (cantidad: number) => 176 - cantidad * 142 / maximo
  const puntos = filas.map((fila, indice) => `${x(indice)},${y(fila.recetas)}`).join(" ")
  const pasoEtiqueta = Math.max(1, Math.ceil(filas.length / 8))

  return (
    <div className="overflow-x-auto">
      <svg
        viewBox="0 0 640 220"
        className="h-56 min-w-[560px] w-full"
        role="img"
        aria-label="Tendencia semanal de recetas"
      >
        {[0, 1, 2, 3].map((marca) => {
          const altura = 176 - marca * 142 / 3
          return (
            <g key={marca}>
              <line x1="32" x2="608" y1={altura} y2={altura} className="stroke-border" />
              <text x="24" y={altura + 4} textAnchor="end" className="fill-muted-foreground text-[10px]">
                {Math.round(maximo * marca / 3)}
              </text>
            </g>
          )
        })}
        <polyline points={puntos} fill="none" className="stroke-primary" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />
        {filas.map((fila, indice) => (
          <g key={fila.semana}>
            <circle cx={x(indice)} cy={y(fila.recetas)} r="4" className="fill-primary">
              <title>{`${fila.semana}: ${numero(fila.recetas)} recetas`}</title>
            </circle>
            {indice % pasoEtiqueta === 0 || indice === filas.length - 1 ? (
              <text x={x(indice)} y="202" textAnchor="middle" className="fill-muted-foreground text-[10px]">
                {`${fila.semana.slice(8)}/${fila.semana.slice(5, 7)}`}
              </text>
            ) : null}
          </g>
        ))}
      </svg>
    </div>
  )
}

export function ReportePrescripcionClinica() {
  const [desde, setDesde] = useState(() => {
    const hoy = new Date()
    return toFechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  })
  const [hasta, setHasta] = useState(() => toFechaISO(new Date()))
  const [especialidadSeleccionada, setEspecialidadSeleccionada] = useState("")
  const [doctorSeleccionado, setDoctorSeleccionado] = useState("")
  const [cargandoArchivo, setCargandoArchivo] = useState(false)
  const [errorArchivo, setErrorArchivo] = useState<string | null>(null)
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null)
  const especialidad = especialidadSeleccionada ? Number(especialidadSeleccionada) : undefined
  const doctor = doctorSeleccionado ? Number(doctorSeleccionado) : undefined
  const dias = desde && hasta ? diferenciaDias(desde, hasta) : -1
  const fechasValidas = dias >= 0 && dias <= 366
  const especialidades = useEspecialidades()
  const doctores = useDoctores(especialidad)
  const consulta = useReportePrescripcion(
    fechasValidas ? desde : null,
    fechasValidas ? hasta : null,
    especialidad,
    doctor
  )
  const reporte = consulta.data

  useEffect(() => () => {
    if (vistaPrevia) URL.revokeObjectURL(vistaPrevia)
  }, [vistaPrevia])

  function invalidarVistaPrevia() {
    setVistaPrevia(null)
    setErrorArchivo(null)
  }

  function cambiarEspecialidad(valor: string) {
    setEspecialidadSeleccionada(valor)
    setDoctorSeleccionado("")
    invalidarVistaPrevia()
  }

  async function obtenerPdf() {
    if (!fechasValidas) return null
    setCargandoArchivo(true)
    setErrorArchivo(null)
    try {
      return await descargarReportePrescripcionPdf(desde, hasta, especialidad, doctor)
    } catch (error) {
      setErrorArchivo(mensajeDeError(error))
      return null
    } finally {
      setCargandoArchivo(false)
    }
  }

  async function previsualizarPdf() {
    const archivo = await obtenerPdf()
    if (archivo) setVistaPrevia(URL.createObjectURL(archivo))
  }

  async function descargarPdf() {
    const archivo = await obtenerPdf()
    if (archivo) guardarArchivo(archivo, `reporte-morbilidad-prescripcion-${desde}-a-${hasta}.pdf`)
  }

  async function descargarCsv() {
    if (!fechasValidas) return
    setCargandoArchivo(true)
    setErrorArchivo(null)
    try {
      const archivo = await descargarReportePrescripcionCsv(desde, hasta, especialidad, doctor)
      guardarArchivo(archivo, `reporte-morbilidad-prescripcion-${desde}-a-${hasta}.csv`)
    } catch (error) {
      setErrorArchivo(mensajeDeError(error))
    } finally {
      setCargandoArchivo(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reporte integral de morbilidad y prescripción médica</CardTitle>
        <CardDescription>
          Analiza diagnósticos, medicamentos recetados y tendencias semanales. Puedes revisar el PDF antes de descargarlo.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field>
            <FieldLabel htmlFor="prescripcion-desde">Desde</FieldLabel>
            <Input
              id="prescripcion-desde"
              type="date"
              value={desde}
              onChange={(evento) => { setDesde(evento.target.value); invalidarVistaPrevia() }}
            />
          </Field>
          <Field data-invalid={!fechasValidas}>
            <FieldLabel htmlFor="prescripcion-hasta">Hasta</FieldLabel>
            <Input
              id="prescripcion-hasta"
              type="date"
              aria-invalid={!fechasValidas}
              value={hasta}
              onChange={(evento) => { setHasta(evento.target.value); invalidarVistaPrevia() }}
            />
            {fechasValidas ? null : (
              <FieldError>
                {dias < 0 ? "La fecha final debe ser igual o posterior a la inicial." : "El periodo máximo es de un año."}
              </FieldError>
            )}
          </Field>
          <label className="flex flex-col gap-2 text-sm font-medium">
            Especialidad
            <select
              className="h-11 rounded-lg border border-input bg-background px-3 text-sm font-normal"
              value={especialidadSeleccionada}
              onChange={(evento) => cambiarEspecialidad(evento.target.value)}
            >
              <option value="">Todas las especialidades</option>
              {(especialidades.data ?? []).map((fila) => (
                <option key={fila.idEspecialidad} value={fila.idEspecialidad}>{fila.nombre}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            Médico
            <select
              className="h-11 rounded-lg border border-input bg-background px-3 text-sm font-normal disabled:opacity-50"
              value={doctorSeleccionado}
              disabled={!especialidad}
              onChange={(evento) => { setDoctorSeleccionado(evento.target.value); invalidarVistaPrevia() }}
            >
              <option value="">{especialidad ? "Todos los médicos" : "Elige una especialidad primero"}</option>
              {(doctores.data ?? []).map((fila) => (
                <option key={fila.idDoctor} value={fila.idDoctor}>{fila.nombreCompleto}</option>
              ))}
            </select>
          </label>
        </div>

        {consulta.error ? (
          <Alert variant="destructive"><AlertDescription>{mensajeDeError(consulta.error)}</AlertDescription></Alert>
        ) : null}
        {especialidades.error ? (
          <Alert variant="destructive"><AlertDescription>{mensajeDeError(especialidades.error)}</AlertDescription></Alert>
        ) : null}
        {doctores.error ? (
          <Alert variant="destructive"><AlertDescription>{mensajeDeError(doctores.error)}</AlertDescription></Alert>
        ) : null}
        {errorArchivo ? (
          <Alert variant="destructive"><AlertDescription>{errorArchivo}</AlertDescription></Alert>
        ) : null}

        {consulta.isLoading ? <Skeleton className="h-64 rounded-xl" /> : reporte ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Kpi titulo="Total de atenciones" valor={numero(reporte.kpis.totalAtenciones)} />
              <Kpi titulo="Medicamentos recetados" valor={numero(reporte.kpis.totalMedicamentos)} />
              <Kpi titulo="Promedio de fármacos por receta" valor={numero(reporte.kpis.promedioMedicamentosPorReceta, 2)} />
              <Kpi titulo="Consultas con receta" valor={`${numero(reporte.kpis.porcentajeConsultasConReceta, 1)} %`} />
            </div>

            <div className="grid gap-4 xl:grid-cols-2">
              <Card>
                <CardHeader><CardTitle>Top 10 medicamentos más recetados</CardTitle></CardHeader>
                <CardContent>
                  <BarrasHorizontales
                    filas={reporte.topMedicamentos}
                    valor={(fila) => fila.cantidad}
                    tituloVacio="No hay medicamentos recetados en este periodo."
                  />
                </CardContent>
              </Card>
              <Card>
                <CardHeader><CardTitle>Diagnósticos más frecuentes</CardTitle></CardHeader>
                <CardContent>
                  <BarrasHorizontales
                    filas={reporte.topDiagnosticos}
                    valor={(fila) => fila.atenciones}
                    tituloVacio="No hay diagnósticos registrados en este periodo."
                  />
                </CardContent>
              </Card>
            </div>

            <Card>
              <CardHeader><CardTitle>Tendencia semanal de recetas</CardTitle></CardHeader>
              <CardContent><GraficoTendencia filas={reporte.tendenciaSemanal} /></CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Prescripciones por médico y especialidad</CardTitle>
              </CardHeader>
              <CardContent>
                {reporte.porMedico.length ? (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Médico</TableHead>
                        <TableHead>Especialidad</TableHead>
                        <TableHead className="text-right">Recetas</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {reporte.porMedico.map((fila) => (
                        <TableRow key={fila.idDoctor}>
                          <TableCell className="font-medium">{fila.doctor}</TableCell>
                          <TableCell>{fila.especialidad}</TableCell>
                          <TableCell className="text-right tabular-nums">{numero(fila.recetas)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                ) : <p className="text-sm text-muted-foreground">No hay recetas registradas por médico en este periodo.</p>}
              </CardContent>
            </Card>

            <div className="flex flex-wrap gap-3">
              <Button variant="outline" className="h-11" disabled={cargandoArchivo} onClick={previsualizarPdf}>
                <EyeIcon data-icon="inline-start" />
                {cargandoArchivo ? "Generando reporte…" : "Previsualizar PDF"}
              </Button>
              <Button variant="outline" className="h-11" disabled={cargandoArchivo} onClick={descargarPdf}>
                <FileTextIcon data-icon="inline-start" />
                Descargar PDF
              </Button>
              <Button className="h-11" disabled={cargandoArchivo} onClick={descargarCsv}>
                <DownloadIcon data-icon="inline-start" />
                Descargar CSV para Excel
              </Button>
            </div>
          </>
        ) : null}

        {vistaPrevia ? (
          <section aria-label="Vista previa del reporte de morbilidad y prescripción" className="space-y-2">
            <h3 className="text-sm font-semibold">Vista previa — {desde} al {hasta}</h3>
            <iframe
              title="Vista previa del reporte de morbilidad y prescripción"
              src={vistaPrevia}
              className="h-[min(75vh,900px)] w-full rounded-lg border bg-background"
            />
          </section>
        ) : null}
      </CardContent>
    </Card>
  )
}

function Kpi({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <Card>
      <CardHeader className="pb-2"><CardDescription>{titulo}</CardDescription></CardHeader>
      <CardContent><p className="text-2xl font-semibold tabular-nums">{valor}</p></CardContent>
    </Card>
  )
}
