"use client"

import { useRef, useState } from "react"
import Link from "next/link"
import { ArrowRightIcon, ChevronLeftIcon, ChevronRightIcon, PauseIcon, PlayIcon } from "lucide-react"

import { Skeleton } from "@/components/ui/skeleton"
import { DIAPOSITIVAS_INSTITUCIONALES, type DiapositivaInstitucional } from "@/components/anuncios/diapositivas"
import { useAnuncios } from "@/hooks/use-anuncios"
import { srcImagenAnuncio } from "@/lib/api/anuncios"
import type { Anuncio } from "@/lib/api/types"
import { cn } from "@/lib/utils"

const DURACION_MS = 7000

type Diapositiva =
  | ({ tipo: "imagen"; clave: string; src: string } & Pick<
      Anuncio,
      "titulo" | "descripcion" | "etiqueta" | "enlace" | "textoBoton"
    >)
  | ({ tipo: "institucional"; clave: string } & DiapositivaInstitucional)

function desdeAnuncio(a: Anuncio): Diapositiva {
  return {
    tipo: "imagen",
    clave: `anuncio-${a.idAnuncio}`,
    src: srcImagenAnuncio(a.imagenUrl),
    titulo: a.titulo,
    descripcion: a.descripcion,
    etiqueta: a.etiqueta,
    enlace: a.enlace,
    textoBoton: a.textoBoton,
  }
}

/**
 * Carrusel de eventos y anuncios del centro de salud (banners de 1200 x 640 px). Muestra los que
 * publica administración; si no hay ninguno vigente, muestra los anuncios institucionales.
 */
export function CarruselEventos({
  className,
  // Solo estos anuncios (vista previa del administrador); sin él se leen los vigentes.
  anuncios: anunciosFijos,
}: {
  className?: string
  anuncios?: Anuncio[]
}) {
  const { data, isLoading } = useAnuncios()
  const anuncios = anunciosFijos ?? data
  const [activa, setActiva] = useState(0)
  const [pausaManual, setPausaManual] = useState(false)
  const [encima, setEncima] = useState(false)
  const inicioToque = useRef<number | null>(null)

  if (!anunciosFijos && isLoading) {
    return <Skeleton className={cn("aspect-[15/8] w-full rounded-2xl", className)} />
  }

  const diapositivas: Diapositiva[] = anuncios?.length
    ? anuncios.map(desdeAnuncio)
    : DIAPOSITIVAS_INSTITUCIONALES.map((d, i) => ({ ...d, tipo: "institucional", clave: `inst-${i}` }))
  const total = diapositivas.length
  const indice = activa % total
  const pausado = pausaManual || encima || total < 2

  const ir = (i: number) => setActiva((i + total) % total)

  return (
    <section
      aria-roledescription="carrusel"
      aria-label="Eventos y novedades del centro de salud"
      className={cn("@container group/carrusel relative w-full", className)}
      onMouseEnter={() => setEncima(true)}
      onMouseLeave={() => setEncima(false)}
      onFocus={() => setEncima(true)}
      onBlur={() => setEncima(false)}
      onPointerDown={(e) => (inicioToque.current = e.clientX)}
      onPointerUp={(e) => {
        if (inicioToque.current === null) return
        const recorrido = e.clientX - inicioToque.current
        inicioToque.current = null
        if (Math.abs(recorrido) > 50) ir(indice + (recorrido < 0 ? 1 : -1))
      }}
    >
      <div className="relative aspect-[15/8] w-full overflow-hidden rounded-2xl bg-navy shadow-[0_12px_40px_rgb(14_60_90/0.18)] ring-1 ring-foreground/10">
        {diapositivas.map((d, i) => (
          <div
            key={d.clave}
            role="group"
            aria-roledescription="diapositiva"
            aria-label={`${i + 1} de ${total}: ${d.titulo}`}
            aria-hidden={i !== indice}
            inert={i !== indice}
            className={cn(
              "absolute inset-0 transition-[opacity,scale] duration-700 ease-out motion-reduce:transition-none",
              i === indice ? "scale-100 opacity-100" : "pointer-events-none scale-[1.03] opacity-0"
            )}
          >
            {d.tipo === "imagen" ? <BannerImagen {...d} /> : <DiapositivaDisenada d={d} />}
          </div>
        ))}

        {total > 1 ? (
          <>
            <BotonFlecha lado="izquierda" onClick={() => ir(indice - 1)} />
            <BotonFlecha lado="derecha" onClick={() => ir(indice + 1)} />
          </>
        ) : null}
      </div>

      {total > 1 ? (
        <div className="mt-3 flex items-center justify-center gap-3">
          <div className="flex items-center gap-1.5" role="group" aria-label="Elegir diapositiva">
            {diapositivas.map((d, i) => (
              <button
                key={d.clave}
                type="button"
                aria-label={`Ir a la diapositiva ${i + 1}: ${d.titulo}`}
                aria-current={i === indice ? "true" : undefined}
                onClick={() => ir(i)}
                className={cn(
                  "relative h-2 overflow-hidden rounded-full bg-foreground/15 outline-none transition-[width] duration-300 hover:bg-foreground/25 focus-visible:ring-3 focus-visible:ring-ring/50",
                  i === indice ? "w-10" : "w-2"
                )}
              >
                {i === indice ? (
                  <span
                    key={activa}
                    aria-hidden="true"
                    className="absolute inset-0 origin-left rounded-full bg-primary motion-reduce:animate-none"
                    style={{
                      animation: `progreso-carrusel ${DURACION_MS}ms linear forwards`,
                      animationPlayState: pausado ? "paused" : "running",
                    }}
                    onAnimationEnd={() => setActiva((a) => a + 1)}
                  />
                ) : null}
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={() => setPausaManual((p) => !p)}
            className="flex size-7 items-center justify-center rounded-full text-muted-foreground outline-none hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {pausaManual ? <PlayIcon className="size-3.5" /> : <PauseIcon className="size-3.5" />}
            <span className="sr-only">{pausaManual ? "Reanudar el carrusel" : "Pausar el carrusel"}</span>
          </button>
        </div>
      ) : null}
    </section>
  )
}

function BotonFlecha({ lado, onClick }: { lado: "izquierda" | "derecha"; onClick: () => void }) {
  const Icono = lado === "izquierda" ? ChevronLeftIcon : ChevronRightIcon
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "absolute top-1/2 z-10 flex size-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/85 text-slate-800 shadow-lg outline-none backdrop-blur transition-opacity hover:bg-white focus-visible:opacity-100 focus-visible:ring-3 focus-visible:ring-white/70 @lg:size-11 @lg:opacity-0 @lg:group-hover/carrusel:opacity-100",
        lado === "izquierda" ? "left-3" : "right-3"
      )}
    >
      <Icono className="size-5" />
      <span className="sr-only">{lado === "izquierda" ? "Anterior" : "Siguiente"}</span>
    </button>
  )
}

function BotonEnlace({
  enlace,
  texto,
  claro,
  vistaPrevia,
}: {
  enlace: string
  texto: string
  claro?: boolean
  vistaPrevia?: boolean
}) {
  const clases = cn(
    "inline-flex h-9 w-fit items-center gap-2 rounded-full px-4 text-sm font-semibold shadow-md outline-none transition-colors focus-visible:ring-3 focus-visible:ring-white/70 @lg:h-11 @lg:px-6 @lg:text-[15px]",
    claro ? "bg-white text-slate-900 hover:bg-white/90" : "bg-lime text-lime-foreground hover:bg-[color-mix(in_oklch,var(--lime),black_8%)]"
  )
  const contenido = (
    <>
      {texto}
      <ArrowRightIcon className="size-4" aria-hidden="true" />
    </>
  )
  if (vistaPrevia) return <span className={clases}>{contenido}</span>
  return enlace.startsWith("/") ? (
    <Link href={enlace} className={clases}>
      {contenido}
    </Link>
  ) : (
    <a href={enlace} target="_blank" rel="noopener noreferrer" className={clases}>
      {contenido}
    </a>
  )
}

/**
 * Banner subido por administración: la imagen completa y el título sobre la franja inferior.
 * Debe ir dentro de un contenedor @container con aspect-[15/8] (lo usa también la vista previa).
 */
export function BannerImagen({
  vistaPrevia,
  ...d
}: Pick<Anuncio, "titulo" | "descripcion" | "etiqueta" | "enlace" | "textoBoton"> & {
  src: string
  // En el formulario del administrador el botón no navega.
  vistaPrevia?: boolean
}) {
  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- la sirve el backend, sin optimizador de Next */}
      <img src={d.src} alt={d.titulo || "Vista previa del banner"} className="absolute inset-0 size-full object-cover" draggable={false} />
      <div className="absolute inset-x-0 bottom-0 flex flex-col gap-1.5 bg-linear-to-t from-black/80 via-black/40 to-transparent px-5 pt-16 pb-5 text-white @lg:gap-2.5 @lg:px-10 @lg:pt-24 @lg:pb-9">
        {d.etiqueta ? (
          <span className="w-fit rounded-full bg-lime px-2.5 py-0.5 text-[11px] font-bold tracking-wide text-lime-foreground uppercase @lg:text-xs">
            {d.etiqueta}
          </span>
        ) : null}
        <h3 className="max-w-[28ch] text-lg leading-tight font-bold text-balance drop-shadow @lg:text-3xl @3xl:text-4xl">
          {d.titulo}
        </h3>
        {d.descripcion ? (
          <p className="hidden max-w-[60ch] text-sm opacity-90 @md:block @lg:text-base">{d.descripcion}</p>
        ) : null}
        {d.enlace ? (
          <div className="mt-1 hidden @sm:block">
            <BotonEnlace enlace={d.enlace} texto={d.textoBoton ?? "Más información"} claro vistaPrevia={vistaPrevia} />
          </div>
        ) : null}
      </div>
    </>
  )
}

/** Anuncio institucional dibujado con CSS, para cuando administración aún no sube banners. */
function DiapositivaDisenada({ d }: { d: DiapositivaInstitucional }) {
  const { Icono } = d
  return (
    <div className={cn("absolute inset-0 overflow-hidden bg-linear-to-br text-white", d.fondo)}>
      {/* Formas decorativas */}
      <span aria-hidden="true" className="absolute -top-1/4 right-[-8%] size-[70%] rounded-full bg-white/10" />
      <span aria-hidden="true" className="absolute right-[18%] -bottom-1/3 size-[45%] rounded-full bg-white/[0.07]" />
      <span aria-hidden="true" className="absolute top-[12%] left-[46%] hidden text-5xl font-thin opacity-40 @lg:block">
        +
      </span>
      <span aria-hidden="true" className="absolute bottom-[14%] left-[4%] hidden text-4xl font-thin opacity-30 @lg:block">
        +
      </span>

      <div className="relative flex h-full items-center gap-4 px-6 @lg:px-12 @3xl:px-16">
        <div className="flex max-w-[62%] flex-col gap-2 @lg:gap-3.5">
          <span className="w-fit rounded-full bg-white/20 px-2.5 py-0.5 text-[11px] font-bold tracking-wide uppercase ring-1 ring-white/30 backdrop-blur @lg:text-xs">
            {d.etiqueta}
          </span>
          <h3 className="text-xl leading-tight font-bold text-balance @lg:text-4xl @3xl:text-5xl">{d.titulo}</h3>
          <p className="hidden text-sm leading-relaxed text-pretty opacity-90 @md:block @lg:text-lg">{d.descripcion}</p>
          {d.dato ? (
            <p className="hidden text-xs font-semibold tracking-wide opacity-80 @2xl:block">{d.dato}</p>
          ) : null}
          {d.enlace ? (
            <div className="mt-1 hidden @sm:block">
              <BotonEnlace enlace={d.enlace} texto={d.textoBoton ?? "Más información"} />
            </div>
          ) : null}
        </div>

        <div aria-hidden="true" className="relative ml-auto flex aspect-square w-[30%] items-center justify-center">
          <span className="absolute inset-0 rounded-full bg-white/15 ring-1 ring-white/25" />
          <span className="absolute inset-[14%] rounded-full bg-white/15" />
          <Icono className="relative size-[46%] drop-shadow-lg" strokeWidth={1.6} />
        </div>
      </div>
    </div>
  )
}
