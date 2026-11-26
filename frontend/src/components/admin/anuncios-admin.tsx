"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import {
  CalendarRangeIcon,
  CircleAlertIcon,
  DownloadIcon,
  EyeIcon,
  EyeOffIcon,
  GalleryHorizontalEndIcon,
  ImageIcon,
  LinkIcon,
  PlusIcon,
  RulerIcon,
  Trash2Icon,
} from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { NuevoAnuncioDialog } from "@/components/admin/nuevo-anuncio-dialog"
import { CarruselEventos } from "@/components/anuncios/carrusel-eventos"
import { DatoHero, HeroSeccion } from "@/components/portal/hero-seccion"
import { useAnunciosAdmin } from "@/hooks/use-admin"
import { cambiarVisibilidadAnuncio, eliminarAnuncio, rutasAdmin } from "@/lib/api/admin"
import { BANNER, RUTA_ANUNCIOS, srcImagenAnuncio } from "@/lib/api/anuncios"
import { mensajeDeError } from "@/lib/api/http"
import type { AnuncioAdmin, EstadoAnuncio } from "@/lib/api/types"
import { formatFecha } from "@/lib/format"

const ESTADOS: Record<EstadoAnuncio, { texto: string; variant: "success" | "warning" | "outline" | "secondary" }> = {
  VIGENTE: { texto: "En el carrusel", variant: "success" },
  PROGRAMADO: { texto: "Programado", variant: "warning" },
  VENCIDO: { texto: "Vencido", variant: "outline" },
  OCULTO: { texto: "Oculto", variant: "secondary" },
}

/** Plantilla PNG de 1200 x 640 con la zona que tapa el título, para el diseñador del banner. */
function descargarPlantilla() {
  const lienzo = document.createElement("canvas")
  lienzo.width = BANNER.ancho
  lienzo.height = BANNER.alto
  const ctx = lienzo.getContext("2d")
  if (!ctx) return
  const fondo = ctx.createLinearGradient(0, 0, BANNER.ancho, BANNER.alto)
  fondo.addColorStop(0, "#e0f2fe")
  fondo.addColorStop(1, "#cffafe")
  ctx.fillStyle = fondo
  ctx.fillRect(0, 0, BANNER.ancho, BANNER.alto)

  // Zona del título y el botón (tercio inferior izquierdo).
  ctx.fillStyle = "rgba(15, 23, 42, 0.18)"
  ctx.fillRect(0, BANNER.alto * 0.62, BANNER.ancho * 0.66, BANNER.alto * 0.38)
  ctx.setLineDash([14, 10])
  ctx.strokeStyle = "#0e7490"
  ctx.lineWidth = 3
  ctx.strokeRect(40, 40, BANNER.ancho - 80, BANNER.alto - 80)

  ctx.fillStyle = "#164e63"
  ctx.font = "bold 56px sans-serif"
  ctx.fillText(`${BANNER.ancho} × ${BANNER.alto} px`, 80, 140)
  ctx.font = "28px sans-serif"
  ctx.fillText("Banner del carrusel de eventos · SaludGrau", 80, 190)
  ctx.fillText("Margen de seguridad: 40 px (línea punteada)", 80, 236)
  ctx.fillStyle = "#0f172a"
  ctx.font = "bold 30px sans-serif"
  ctx.fillText("Zona del título y el botón: evita texto importante aquí", 60, BANNER.alto * 0.62 + 70)

  const enlace = document.createElement("a")
  enlace.href = lienzo.toDataURL("image/png")
  enlace.download = `plantilla-banner-${BANNER.ancho}x${BANNER.alto}.png`
  enlace.click()
}

export function AnunciosAdmin() {
  const { data: anuncios, error, isLoading } = useAnunciosAdmin()
  const [nuevo, setNuevo] = useState(false)

  const vigentes = anuncios?.filter((a) => a.estado === "VIGENTE") ?? []
  const contar = (estado: EstadoAnuncio) => anuncios?.filter((a) => a.estado === estado).length ?? "…"

  return (
    <>
      <HeroSeccion
        etiqueta="Comunicación"
        titulo="Carrusel de eventos"
        descripcion="Publica banners de eventos, nuevas especialidades y avisos del centro. Los pacientes los ven en Campañas y en el inicio de su portal."
        Icono={GalleryHorizontalEndIcon}
        pie={
          <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <DatoHero Icono={EyeIcon} valor={isLoading ? "…" : vigentes.length} texto="En el carrusel hoy" />
            <DatoHero Icono={CalendarRangeIcon} valor={contar("PROGRAMADO")} texto="Programados" />
            <DatoHero Icono={EyeOffIcon} valor={contar("OCULTO")} texto="Ocultos" />
            <DatoHero Icono={ImageIcon} valor={anuncios?.length ?? "…"} texto="Banners en total" />
          </ul>
        }
      >
        <Button size="xl" variant="lime" className="rounded-full" onClick={() => setNuevo(true)}>
          <PlusIcon data-icon="inline-start" />
          Nuevo banner
        </Button>
      </HeroSeccion>

      <div className="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_360px] xl:gap-8">
        <Card>
          <CardHeader>
            <CardTitle className="text-xl font-bold">Así lo ven los pacientes</CardTitle>
            <CardDescription>
              {vigentes.length
                ? "Vista previa con los banners vigentes de hoy, en el mismo orden que el portal."
                : "Aún no hay banners vigentes: el portal muestra los anuncios institucionales del centro."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="aspect-[15/8] w-full rounded-2xl" />
            ) : (
              <CarruselEventos anuncios={vigentes.length ? vigentes : []} />
            )}
          </CardContent>
        </Card>

        <EspecificacionesCard />
      </div>

      <section aria-labelledby="banners-publicados" className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <h2 id="banners-publicados" className="text-2xl font-bold tracking-tight">
            Banners publicados
          </h2>
          <p className="text-muted-foreground">
            Oculta un banner para sacarlo del carrusel sin perderlo, o elimínalo si ya no lo necesitas.
          </p>
        </div>

        {isLoading ? (
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-80 rounded-xl" />
            ))}
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{mensajeDeError(error)}</AlertDescription>
          </Alert>
        ) : !anuncios?.length ? (
          <Card>
            <CardContent>
              <Empty className="py-12">
                <EmptyHeader>
                  <EmptyMedia variant="icon">
                    <GalleryHorizontalEndIcon />
                  </EmptyMedia>
                  <EmptyTitle>Todavía no publicas banners</EmptyTitle>
                  <EmptyDescription>
                    Sube el primero con el botón Nuevo banner. Usa una imagen de {BANNER.ancho} × {BANNER.alto} px.
                  </EmptyDescription>
                </EmptyHeader>
                <Button size="lg" onClick={() => setNuevo(true)}>
                  <PlusIcon data-icon="inline-start" />
                  Nuevo banner
                </Button>
              </Empty>
            </CardContent>
          </Card>
        ) : (
          <ul className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {anuncios.map((a) => (
              <li key={a.idAnuncio}>
                <TarjetaAnuncio anuncio={a} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <NuevoAnuncioDialog open={nuevo} onOpenChange={setNuevo} />
    </>
  )
}

function EspecificacionesCard() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 font-bold">
          <RulerIcon className="size-5 text-primary" aria-hidden="true" />
          Medidas del banner
        </CardTitle>
        <CardDescription>Diseña la imagen con estas medidas para que no se recorte.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        {/* Esquema a escala 15:8 */}
        <figure className="flex flex-col gap-2">
          <div className="relative aspect-[15/8] w-full overflow-hidden rounded-xl bg-linear-to-br from-sky-100 to-cyan-100 ring-1 ring-primary/30 dark:from-sky-950 dark:to-cyan-950">
            <span className="absolute inset-[6.25%] rounded-md border-2 border-dashed border-primary/60" aria-hidden="true" />
            <span className="absolute inset-x-0 top-0 flex justify-center pt-1.5 text-[11px] font-bold text-primary">
              {BANNER.ancho} px
            </span>
            <span className="absolute top-1/2 right-1 -translate-y-1/2 text-[11px] font-bold text-primary [writing-mode:vertical-rl]">
              {BANNER.alto} px
            </span>
            <span className="absolute bottom-0 left-0 flex h-[38%] w-[66%] items-end bg-slate-900/25 p-2 text-[10px] leading-tight font-semibold text-slate-900 dark:text-white">
              Título y botón del portal
            </span>
          </div>
          <figcaption className="text-xs text-muted-foreground">
            La línea punteada es el margen de seguridad; la zona oscura queda bajo el título.
          </figcaption>
        </figure>

        <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-2 text-sm">
          <dt className="text-muted-foreground">Tamaño ideal</dt>
          <dd className="font-semibold">
            {BANNER.ancho} × {BANNER.alto} px
          </dd>
          <dt className="text-muted-foreground">Proporción</dt>
          <dd className="font-semibold">15:8 (rectangular)</dd>
          <dt className="text-muted-foreground">Mínimo</dt>
          <dd className="font-semibold">
            {BANNER.anchoMinimo} × {BANNER.altoMinimo} px
          </dd>
          <dt className="text-muted-foreground">Formato</dt>
          <dd className="font-semibold">JPG o PNG, hasta 5 MB</dd>
        </dl>

        <Button variant="outline" size="lg" className="w-full" onClick={descargarPlantilla}>
          <DownloadIcon data-icon="inline-start" />
          Descargar plantilla {BANNER.ancho} × {BANNER.alto}
        </Button>
      </CardContent>
    </Card>
  )
}

function TarjetaAnuncio({ anuncio: a }: { anuncio: AnuncioAdmin }) {
  const { mutate } = useSWRConfig()
  const [cambiando, startCambio] = useTransition()
  const [eliminando, startEliminar] = useTransition()
  const [confirmar, setConfirmar] = useState(false)
  const estado = ESTADOS[a.estado]

  const refrescar = () => Promise.all([mutate(rutasAdmin.anuncios), mutate(RUTA_ANUNCIOS)])

  function alternar() {
    startCambio(async () => {
      try {
        await cambiarVisibilidadAnuncio(a.idAnuncio, !a.activo)
        await refrescar()
        toast.success(a.activo ? "Banner oculto del carrusel." : "Banner visible otra vez en el carrusel.")
      } catch (error) {
        toast.error(mensajeDeError(error))
      }
    })
  }

  function eliminar() {
    startEliminar(async () => {
      try {
        await eliminarAnuncio(a.idAnuncio)
        await refrescar()
        setConfirmar(false)
        toast.success("Banner eliminado.")
      } catch (error) {
        toast.error(mensajeDeError(error))
      }
    })
  }

  return (
    <Card className="h-full gap-0 py-0">
      <div className="relative aspect-[15/8] w-full overflow-hidden bg-muted">
        {/* eslint-disable-next-line @next/next/no-img-element -- la sirve el backend */}
        <img
          src={srcImagenAnuncio(a.imagenUrl)}
          alt={a.titulo}
          className={a.activo ? "size-full object-cover" : "size-full object-cover opacity-50 grayscale"}
        />
        <Badge variant={estado.variant} className="absolute top-3 left-3 shadow-sm">
          {estado.texto}
        </Badge>
        <span className="absolute right-3 bottom-3 rounded-full bg-black/60 px-2 py-0.5 text-[11px] font-medium text-white">
          {a.anchoImagen} × {a.altoImagen}
        </span>
      </div>
      <CardContent className="flex flex-1 flex-col gap-3 py-5">
        <div className="flex flex-col gap-1">
          {a.etiqueta ? <span className="text-xs font-semibold text-primary uppercase">{a.etiqueta}</span> : null}
          <h3 className="leading-snug font-bold">{a.titulo}</h3>
          {a.descripcion ? <p className="line-clamp-2 text-sm text-muted-foreground">{a.descripcion}</p> : null}
        </div>
        <ul className="flex flex-col gap-1 text-xs text-muted-foreground">
          <li className="flex items-center gap-1.5">
            <CalendarRangeIcon className="size-3.5 shrink-0" aria-hidden="true" />
            {formatFecha(a.fechaInicio)} al {formatFecha(a.fechaFin)}
          </li>
          {a.enlace ? (
            <li className="flex min-w-0 items-center gap-1.5">
              <LinkIcon className="size-3.5 shrink-0" aria-hidden="true" />
              <span className="truncate">
                {a.textoBoton} → {a.enlace}
              </span>
            </li>
          ) : null}
        </ul>
        <div className="mt-auto flex gap-2 pt-1">
          <Button variant="outline" size="lg" className="flex-1" disabled={cambiando} onClick={alternar}>
            {cambiando ? (
              <Spinner data-icon="inline-start" />
            ) : a.activo ? (
              <EyeOffIcon data-icon="inline-start" />
            ) : (
              <EyeIcon data-icon="inline-start" />
            )}
            {a.activo ? "Ocultar" : "Mostrar"}
          </Button>
          <Button variant="destructive" size="lg" onClick={() => setConfirmar(true)}>
            <Trash2Icon data-icon="inline-start" />
            Eliminar
          </Button>
        </div>
      </CardContent>

      <AlertDialog open={confirmar} onOpenChange={setConfirmar}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar este banner?</AlertDialogTitle>
            <AlertDialogDescription>
              &quot;{a.titulo}&quot; saldrá del carrusel y se borrará su imagen. Si solo quieres quitarlo por un
              tiempo, mejor ocúltalo.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={eliminando}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={eliminando}
              onClick={(e) => {
                e.preventDefault()
                eliminar()
              }}
            >
              {eliminando ? <Spinner data-icon="inline-start" /> : null}
              Eliminar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  )
}
