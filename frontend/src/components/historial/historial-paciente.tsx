"use client"

import { useState } from "react"
import Link from "next/link"
import {
  ArrowRightIcon,
  BadgeCheckIcon,
  CalendarXIcon,
  ChevronDownIcon,
  CircleAlertIcon,
  ClipboardListIcon,
  DownloadIcon,
  FileBadgeIcon,
  FileHeartIcon,
  FileTextIcon,
  FlaskConicalIcon,
  FolderOpenIcon,
  InfoIcon,
  MonitorSmartphoneIcon,
  type LucideIcon,
  PillIcon,
  ScrollTextIcon,
  SendIcon,
  StethoscopeIcon,
} from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { DatoHero, HeroSeccion } from "@/components/portal/hero-seccion"
import { useDocumentos, useHistorial } from "@/hooks/use-citas"
import {
  descargarCertificadoPaciente,
  descargarConstanciaPaciente,
  descargarDocumento,
  descargarHistorialClinico,
  descargarOrdenExamen,
  descargarReceta,
  descargarReferenciaPaciente,
} from "@/lib/api/citas"
import { mensajeDeError } from "@/lib/api/http"
import type { AtencionHistorial, DocumentoMedico } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"
import { formatFecha, parseFecha } from "@/lib/format"
import { cn } from "@/lib/utils"

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
const POR_PAGINA = 5
const DOCUMENTOS_VISIBLES = 5

type Filtro = "TODAS" | "ATENDIDA" | "NO_ASISTIO"

/** Una descarga a la vez: la clave identifica qué botón muestra el indicador. */
function useDescargas() {
  const [descargando, setDescargando] = useState<string | null>(null)

  async function descargar(clave: string, obtener: () => Promise<Blob>, nombre: string) {
    setDescargando(clave)
    try {
      guardarArchivo(await obtener(), nombre)
    } catch (error) {
      toast.error(mensajeDeError(error))
    } finally {
      setDescargando(null)
    }
  }

  return { descargando, descargar }
}

// RF-09 (historial de asistencias) y RF-11 (descarga de documentos).
export function HistorialPaciente() {
  const historial = useHistorial()
  const documentos = useDocumentos()
  const { descargando, descargar } = useDescargas()

  const atenciones = historial.data ?? []
  const atendidas = atenciones.filter((a) => a.estado === "ATENDIDA").length
  const referencias = atenciones.filter((a) => a.tieneDerivacion && a.idAtencion !== null).length
  const cargando = historial.isLoading

  return (
    <>
      <HeroSeccion
        etiqueta="Portal del paciente"
        titulo="Historial médico"
        descripcion="Todas tus atenciones en un solo lugar. Descarga constancias, certificados, recetas, órdenes de exámenes y los documentos que el personal de salud cargó para ti."
        Icono={FileHeartIcon}
        pie={
          <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <DatoHero Icono={StethoscopeIcon} valor={cargando ? "…" : atendidas} texto="Atenciones registradas" />
            <DatoHero
              Icono={CalendarXIcon}
              valor={cargando ? "…" : atenciones.length - atendidas}
              texto="Citas sin asistencia"
            />
            <DatoHero
              Icono={FolderOpenIcon}
              valor={documentos.isLoading ? "…" : (documentos.data?.length ?? 0)}
              texto="Documentos cargados"
            />
            <DatoHero Icono={SendIcon} valor={cargando ? "…" : referencias} texto="Referencias emitidas" />
          </ul>
        }
      >
        <Button
          size="xl"
          variant="lime"
          className="rounded-full"
          disabled={descargando === "historial"}
          onClick={() => descargar("historial", descargarHistorialClinico, "historial-clinico.pdf")}
        >
          {descargando === "historial" ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
          Descargar historial completo
        </Button>
      </HeroSeccion>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_360px] lg:gap-8">
        <ListaAtenciones
          atenciones={atenciones}
          cargando={cargando}
          error={historial.error}
          descargando={descargando}
          descargar={descargar}
        />

        <aside className="flex flex-col gap-6 lg:sticky lg:top-32">
          <DocumentosCard
            documentos={documentos.data}
            cargando={documentos.isLoading}
            error={documentos.error}
            descargando={descargando}
            descargar={descargar}
          />
          <PromoTelesalud />
          <GuiaDocumentos />
        </aside>
      </div>
    </>
  )
}

// ----- Atenciones -----

function ListaAtenciones({
  atenciones,
  cargando,
  error,
  descargando,
  descargar,
}: {
  atenciones: AtencionHistorial[]
  cargando: boolean
  error: unknown
} & ReturnType<typeof useDescargas>) {
  const [filtro, setFiltro] = useState<Filtro>("TODAS")
  const [visibles, setVisibles] = useState(POR_PAGINA)

  const filtradas = filtro === "TODAS" ? atenciones : atenciones.filter((a) => a.estado === filtro)
  const mostradas = filtradas.slice(0, visibles)

  // Agrupadas por año, en el orden que llegan (más reciente primero).
  const porAnio: { anio: number; items: AtencionHistorial[] }[] = []
  for (const a of mostradas) {
    const anio = parseFecha(a.fecha).getFullYear()
    const grupo = porAnio.at(-1)
    if (grupo?.anio === anio) grupo.items.push(a)
    else porAnio.push({ anio, items: [a] })
  }

  return (
    <Card className="gap-0">
      <CardHeader className="gap-4 border-b pb-5 sm:grid-cols-[1fr_auto] lg:pb-6">
        <div className="flex flex-col gap-1">
          <CardTitle className="text-xl font-bold">Tus atenciones</CardTitle>
          <CardDescription>Cada atención tiene sus propios documentos para descargar.</CardDescription>
        </div>
        <ToggleGroup
          type="single"
          variant="outline"
          size="sm"
          value={filtro}
          onValueChange={(v) => {
            if (!v) return
            setFiltro(v as Filtro)
            setVisibles(POR_PAGINA)
          }}
          aria-label="Filtrar atenciones"
          className="self-start"
        >
          <ToggleGroupItem value="TODAS" className="px-3">
            Todas
          </ToggleGroupItem>
          <ToggleGroupItem value="ATENDIDA" className="px-3">
            Atendidas
          </ToggleGroupItem>
          <ToggleGroupItem value="NO_ASISTIO" className="px-3">
            No asistí
          </ToggleGroupItem>
        </ToggleGroup>
      </CardHeader>

      <CardContent className="pt-5 lg:pt-6">
        {cargando ? (
          <div className="flex flex-col gap-4">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-36 rounded-2xl" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !filtradas.length ? (
          <Empty className="border border-dashed py-12">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <ClipboardListIcon />
              </EmptyMedia>
              <EmptyTitle>{atenciones.length ? "Nada con este filtro" : "Aún no tienes atenciones"}</EmptyTitle>
              <EmptyDescription>
                {atenciones.length
                  ? "Prueba con otro filtro para ver el resto de tus citas."
                  : "Cuando te atiendas en el centro de salud, tus documentos aparecerán aquí."}
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="flex flex-col gap-7">
            {porAnio.map(({ anio, items }) => (
              <section key={anio} aria-labelledby={`anio-${anio}`} className="flex flex-col gap-4">
                <h3 id={`anio-${anio}`} className="flex items-center gap-3 text-sm font-bold text-muted-foreground">
                  {anio}
                  <span aria-hidden="true" className="h-px flex-1 bg-border" />
                </h3>
                <ol className="relative flex flex-col gap-4 pl-6 before:absolute before:top-2 before:bottom-2 before:left-[7px] before:w-0.5 before:rounded-full before:bg-border">
                  {items.map((a) => (
                    <li key={a.idCita} className="relative">
                      <span
                        aria-hidden="true"
                        className={cn(
                          "absolute top-6 -left-6 size-4 rounded-full border-[3px] border-card ring-2",
                          a.estado === "ATENDIDA" ? "bg-primary ring-primary/30" : "bg-muted-foreground/40 ring-border"
                        )}
                      />
                      <TarjetaAtencion atencion={a} descargando={descargando} descargar={descargar} />
                    </li>
                  ))}
                </ol>
              </section>
            ))}

            {filtradas.length > visibles ? (
              <Button
                variant="outline"
                size="xl"
                className="self-center rounded-full"
                onClick={() => setVisibles((v) => v + POR_PAGINA)}
              >
                <ChevronDownIcon data-icon="inline-start" />
                Ver más atenciones ({filtradas.length - visibles} restantes)
              </Button>
            ) : null}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function FechaBloque({ fecha, atendida }: { fecha: string; atendida: boolean }) {
  const d = parseFecha(fecha)
  return (
    <div
      className={cn(
        "flex w-16 shrink-0 flex-col items-center justify-center rounded-xl py-2 leading-none",
        atendida ? "bg-secondary text-secondary-foreground" : "bg-muted text-muted-foreground"
      )}
    >
      <span className="text-2xl font-bold tabular-nums">{d.getDate()}</span>
      <span className="mt-1 text-xs font-semibold uppercase">{MESES[d.getMonth()]}</span>
    </div>
  )
}

function TarjetaAtencion({
  atencion: a,
  descargando,
  descargar,
}: { atencion: AtencionHistorial } & ReturnType<typeof useDescargas>) {
  const atendida = a.estado === "ATENDIDA"

  const documentos: { clave: string; texto: string; detalle: string; Icono: LucideIcon; tono: string; accion: () => void }[] = []
  if (atendida) {
    documentos.push(
      {
        clave: `constancia-${a.idCita}`,
        texto: "Constancia",
        detalle: "De atención",
        Icono: BadgeCheckIcon,
        tono: "bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300",
        accion: () =>
          descargar(`constancia-${a.idCita}`, () => descargarConstanciaPaciente(a.idCita), `constancia-${a.idCita}.pdf`),
      },
      {
        clave: `certificado-${a.idCita}`,
        texto: "Certificado",
        detalle: "Médico",
        Icono: FileBadgeIcon,
        tono: "bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300",
        accion: () =>
          descargar(
            `certificado-${a.idCita}`,
            () => descargarCertificadoPaciente(a.idCita),
            `certificado-${a.idCita}.pdf`
          ),
      },
      {
        clave: `receta-${a.idCita}`,
        texto: "Receta",
        detalle: "Medicamentos",
        Icono: PillIcon,
        tono: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300",
        accion: () => descargar(`receta-${a.idCita}`, () => descargarReceta(a.idCita), `receta-${a.idCita}.pdf`),
      }
    )
    if (a.tieneOrdenExamen) {
      documentos.push({
        clave: `orden-${a.idCita}`,
        texto: "Orden de exámenes",
        detalle: "Laboratorio",
        Icono: FlaskConicalIcon,
        tono: "bg-violet-100 text-violet-700 dark:bg-violet-950 dark:text-violet-300",
        accion: () =>
          descargar(`orden-${a.idCita}`, () => descargarOrdenExamen(a.idCita), `orden-examenes-${a.idCita}.pdf`),
      })
    }
  }
  if (a.tieneDerivacion && a.idAtencion !== null) {
    const idAtencion = a.idAtencion
    documentos.push({
      clave: `referencia-${idAtencion}`,
      texto: "Referencia",
        detalle: "Derivación",
      Icono: SendIcon,
      tono: "bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300",
      accion: () =>
        descargar(
          `referencia-${idAtencion}`,
          () => descargarReferenciaPaciente(idAtencion),
          `referencia-${idAtencion}.pdf`
        ),
    })
  }

  return (
    <article className="flex flex-col gap-4 rounded-2xl border bg-card p-4 transition-shadow hover:shadow-[0_8px_28px_rgb(14_60_90/0.08)] sm:p-5">
      <div className="flex items-center gap-4">
        <FechaBloque fecha={a.fecha} atendida={atendida} />
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2">
            <h4 className="text-base font-semibold">{a.especialidad}</h4>
            <EstadoCitaBadge estado={a.estado} />
          </div>
          <p className="truncate text-sm text-muted-foreground">{a.doctor}</p>
          <p className="text-xs text-muted-foreground">{formatFecha(a.fecha)}</p>
        </div>
      </div>

      {documentos.length ? (
        <ul className="grid grid-cols-1 gap-2 min-[480px]:grid-cols-2 xl:grid-cols-3">
          {documentos.map(({ clave, texto, detalle, Icono, tono, accion }) => (
            <li key={clave}>
              <button
                type="button"
                onClick={accion}
                disabled={descargando === clave}
                className="group/doc flex w-full items-center gap-3 rounded-xl border bg-background p-2.5 text-left outline-none transition-colors hover:border-primary/40 hover:bg-secondary/60 focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-70"
              >
                <span className={cn("flex size-9 shrink-0 items-center justify-center rounded-lg", tono)}>
                  <Icono className="size-[18px]" aria-hidden="true" />
                </span>
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate text-[13px] font-semibold">{texto}</span>
                  <span className="text-xs text-muted-foreground">{detalle} · PDF</span>
                </span>
                {descargando === clave ? (
                  <Spinner className="size-4 text-primary" />
                ) : (
                  <DownloadIcon
                    className="size-4 text-muted-foreground transition-colors group-hover/doc:text-primary"
                    aria-hidden="true"
                  />
                )}
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-xl bg-muted/60 px-3.5 py-2.5 text-sm text-muted-foreground">
          No asististe a esta cita, por eso no tiene documentos.{" "}
          <Link href="/paciente/agendar" className="font-semibold text-primary hover:underline">
            Agenda una nueva cita
          </Link>
          .
        </p>
      )}
    </article>
  )
}

// ----- Barra lateral -----

function DocumentosCard({
  documentos,
  cargando,
  error,
  descargando,
  descargar,
}: {
  documentos: DocumentoMedico[] | undefined
  cargando: boolean
  error: unknown
} & ReturnType<typeof useDescargas>) {
  const [todos, setTodos] = useState(false)
  const lista = documentos ?? []
  const mostrados = todos ? lista : lista.slice(0, DOCUMENTOS_VISIBLES)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 font-semibold">
          <FolderOpenIcon className="size-5 text-primary" aria-hidden="true" />
          Resultados y documentos
        </CardTitle>
        <CardDescription>Archivos que el personal de salud cargó a tus atenciones.</CardDescription>
      </CardHeader>
      <CardContent>
        {cargando ? (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !lista.length ? (
          <p className="rounded-xl border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">
            Cuando el personal cargue tus resultados o recetas, podrás descargarlos aquí.
          </p>
        ) : (
          <div className="flex flex-col gap-3">
            <ul className="flex flex-col gap-2">
              {mostrados.map((doc) => {
                const clave = `doc-${doc.idDocumento}`
                return (
                  <li key={doc.idDocumento}>
                    <button
                      type="button"
                      disabled={descargando === clave}
                      onClick={() => descargar(clave, () => descargarDocumento(doc.idDocumento), doc.nombreArchivo)}
                      className="flex w-full items-center gap-3 rounded-xl bg-muted/60 p-2.5 text-left outline-none transition-colors hover:bg-secondary focus-visible:ring-3 focus-visible:ring-ring/50"
                    >
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-card text-primary shadow-xs">
                        <FileTextIcon className="size-[18px]" aria-hidden="true" />
                      </span>
                      <span className="flex min-w-0 flex-1 flex-col">
                        <span className="truncate text-[13px] font-semibold">{doc.tipoDocumento}</span>
                        <span className="truncate text-xs text-muted-foreground">
                          {doc.fechaCarga ? formatFecha(doc.fechaCarga.slice(0, 10)) : doc.nombreArchivo}
                        </span>
                      </span>
                      {descargando === clave ? (
                        <Spinner className="size-4 text-primary" />
                      ) : (
                        <DownloadIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                      )}
                      <span className="sr-only">Descargar {doc.tipoDocumento}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
            {lista.length > DOCUMENTOS_VISIBLES ? (
              <Button variant="ghost" size="sm" className="self-center text-primary" onClick={() => setTodos((t) => !t)}>
                {todos ? "Ver menos" : `Ver todos (${lista.length})`}
              </Button>
            ) : null}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function PromoTelesalud() {
  return (
    <section className="relative overflow-hidden rounded-2xl bg-linear-to-br from-violet-700 via-indigo-600 to-sky-500 p-6 text-white shadow-[0_10px_30px_rgb(80_60_200/0.25)]">
      <span aria-hidden="true" className="absolute -top-10 -right-10 size-36 rounded-full bg-white/15" />
      <MonitorSmartphoneIcon aria-hidden="true" className="absolute right-4 bottom-4 size-20 opacity-20" strokeWidth={1.5} />
      <div className="relative flex flex-col gap-2">
        <span className="w-fit rounded-full bg-white/20 px-2.5 py-0.5 text-[11px] font-bold tracking-wide uppercase">
          Nuevo servicio
        </span>
        <h2 className="text-xl leading-tight font-bold">Telesalud: tu control sin salir de casa</h2>
        <p className="text-sm text-white/90">
          Para controles, lectura de resultados y seguimiento de tratamientos. Elige Telesalud al agendar y te
          atendemos por videollamada.
        </p>
        <Button asChild variant="secondary" className="mt-2 h-10 w-fit rounded-full bg-white px-5 text-indigo-700 hover:bg-white/90">
          <Link href="/paciente/agendar">
            Agendar teleconsulta
            <ArrowRightIcon data-icon="inline-end" />
          </Link>
        </Button>
      </div>
    </section>
  )
}

const GUIA: { Icono: LucideIcon; titulo: string; texto: string }[] = [
  { Icono: BadgeCheckIcon, titulo: "Constancia", texto: "Prueba que te atendiste; sirve para tu trabajo o centro de estudios." },
  { Icono: FileBadgeIcon, titulo: "Certificado", texto: "Indica tu diagnóstico y los días de descanso médico, si los hay." },
  { Icono: PillIcon, titulo: "Receta", texto: "Medicamentos, dosis y duración del tratamiento indicados por tu doctor." },
  { Icono: ScrollTextIcon, titulo: "Orden de exámenes", texto: "Preséntala en laboratorio para tomarte las muestras indicadas." },
]

function GuiaDocumentos() {
  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base font-semibold">
          <InfoIcon className="size-4 text-primary" aria-hidden="true" />
          ¿Qué documento necesito?
        </CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="flex flex-col gap-3">
          {GUIA.map(({ Icono, titulo, texto }) => (
            <li key={titulo} className="flex gap-3">
              <Icono className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              <p className="text-[13px] leading-relaxed">
                <span className="font-semibold">{titulo}:</span> <span className="text-muted-foreground">{texto}</span>
              </p>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
