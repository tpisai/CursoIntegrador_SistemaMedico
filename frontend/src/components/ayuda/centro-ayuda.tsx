"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import {
  BuildingIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  LifeBuoyIcon,
  PhoneCallIcon,
  PlayIcon,
  SirenIcon,
} from "lucide-react"

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { CONTENIDO_AYUDA, type PerfilAyuda, type Tutorial } from "@/components/ayuda/contenido"
import { cn } from "@/lib/utils"

const SECCIONES = [
  { id: "tutoriales", texto: "Tutoriales" },
  { id: "guias", texto: "Guías paso a paso" },
  { id: "preguntas", texto: "Preguntas frecuentes" },
  { id: "canales", texto: "Otros canales de atención" },
]

/** Centro de ayuda: tutoriales en carrusel, guías paso a paso, preguntas frecuentes y canales de contacto. */
export function CentroAyuda({ perfil }: { perfil: PerfilAyuda }) {
  const contenido = CONTENIDO_AYUDA[perfil]
  const [tutorial, setTutorial] = useState<Tutorial | null>(null)

  return (
    <div className="flex flex-col gap-10">
      {/* Portada */}
      <section className="relative overflow-hidden rounded-3xl bg-navy px-6 py-10 text-navy-foreground sm:px-10">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -top-16 -right-10 size-64 rounded-full bg-cyan-400/25 blur-3xl"
        />
        <div className="relative flex flex-col items-start gap-6 sm:flex-row sm:items-center">
          <span className="flex size-20 shrink-0 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20">
            <LifeBuoyIcon className="size-10 text-cyan-300" aria-hidden="true" />
          </span>
          <div className="flex flex-1 flex-col gap-2">
            <h1 className="text-3xl font-bold sm:text-4xl">{contenido.titulo}</h1>
            <p className="max-w-2xl text-lg opacity-85">{contenido.descripcion}</p>
          </div>
        </div>
        <nav aria-label="Secciones de ayuda" className="relative mt-8">
          <ul className="flex flex-wrap gap-2">
            {SECCIONES.map((s) => (
              <li key={s.id}>
                <a
                  href={`#${s.id}`}
                  className="flex h-10 items-center rounded-full bg-white/10 px-4 text-sm font-medium ring-1 ring-white/20 outline-none hover:bg-white/20 focus-visible:ring-3 focus-visible:ring-cyan-300"
                >
                  {s.texto}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      </section>

      {/* Tutoriales */}
      <section
        id="tutoriales"
        aria-labelledby="titulo-tutoriales"
        className="scroll-mt-24 rounded-3xl bg-navy px-4 py-10 text-navy-foreground sm:px-8"
      >
        <h2 id="titulo-tutoriales" className="mb-8 text-center text-3xl font-bold text-cyan-300 sm:text-4xl">
          Tutoriales
        </h2>
        <CarruselTutoriales tutoriales={contenido.tutoriales} onAbrir={setTutorial} />
      </section>

      {/* Guías paso a paso */}
      <section id="guias" aria-labelledby="titulo-guias" className="scroll-mt-24">
        <h2 id="titulo-guias" className="mb-4 text-2xl font-semibold">
          Guías paso a paso
        </h2>
        <Accordion type="multiple" className="rounded-2xl bg-card px-5 ring-1 ring-foreground/[0.07]">
          {contenido.guias.map((guia) => (
            <AccordionItem key={guia.titulo} value={guia.titulo}>
              <AccordionTrigger className="py-4 text-base">{guia.titulo}</AccordionTrigger>
              <AccordionContent>
                <ListaPasos pasos={guia.pasos} />
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </section>

      {/* Preguntas frecuentes */}
      <section id="preguntas" aria-labelledby="titulo-preguntas" className="scroll-mt-24">
        <h2 id="titulo-preguntas" className="mb-4 text-2xl font-semibold">
          Preguntas frecuentes
        </h2>
        <Accordion type="single" collapsible className="rounded-2xl bg-card px-5 ring-1 ring-foreground/[0.07]">
          {contenido.preguntas.map((p) => (
            <AccordionItem key={p.pregunta} value={p.pregunta}>
              <AccordionTrigger className="py-4 text-base">{p.pregunta}</AccordionTrigger>
              <AccordionContent className="text-[15px] leading-relaxed text-muted-foreground">
                {p.respuesta}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </section>

      {/* Otros canales */}
      <section id="canales" aria-labelledby="titulo-canales" className="scroll-mt-24">
        <h2 id="titulo-canales" className="mb-4 text-2xl font-semibold">
          Otros canales de atención
        </h2>
        <ul className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Canal
            Icono={BuildingIcon}
            titulo="Atención presencial"
            texto="Acércate al módulo de Admisión del Centro de Salud Miguel Grau, Chaclacayo."
          />
          <Canal
            Icono={PhoneCallIcon}
            titulo="Infosalud · 113"
            texto="Línea gratuita del Ministerio de Salud para orientación médica."
            href="tel:113"
            enlace="Llamar al 113"
          />
          <Canal
            Icono={SirenIcon}
            titulo="Emergencias · SAMU 106"
            texto="Ante una emergencia no reserves cita: llama al 106 o acude a Emergencia."
            href="tel:106"
            enlace="Llamar al 106"
            urgente
          />
        </ul>
      </section>

      <Dialog open={tutorial !== null} onOpenChange={(abierto) => !abierto && setTutorial(null)}>
        <DialogContent className="sm:max-w-lg">
          {tutorial ? (
            <>
              <DialogHeader>
                <DialogTitle className="text-xl">{tutorial.pregunta}</DialogTitle>
                <DialogDescription>Sigue estos pasos en el portal.</DialogDescription>
              </DialogHeader>
              <ListaPasos pasos={tutorial.pasos} />
            </>
          ) : null}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function ListaPasos({ pasos }: { pasos: string[] }) {
  return (
    <ol className="flex flex-col gap-3 pb-1">
      {pasos.map((paso, i) => (
        <li key={paso} className="flex gap-3 text-[15px] leading-relaxed">
          <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
            {i + 1}
          </span>
          <span className="pt-0.5">{paso}</span>
        </li>
      ))}
    </ol>
  )
}

function Canal({
  Icono,
  titulo,
  texto,
  href,
  enlace,
  urgente = false,
}: {
  Icono: React.ComponentType<{ className?: string }>
  titulo: string
  texto: string
  href?: string
  enlace?: string
  urgente?: boolean
}) {
  return (
    <li className="flex flex-col gap-2 rounded-2xl bg-card p-5 ring-1 ring-foreground/[0.07]">
      <span
        className={cn(
          "flex size-11 items-center justify-center rounded-xl",
          urgente ? "bg-destructive/10 text-destructive" : "bg-accent text-accent-foreground"
        )}
      >
        <Icono className="size-5" aria-hidden="true" />
      </span>
      <h3 className="text-lg font-semibold">{titulo}</h3>
      <p className="flex-1 text-sm leading-relaxed text-muted-foreground">{texto}</p>
      {href ? (
        <a
          href={href}
          className={cn(
            "mt-1 text-sm font-semibold outline-none hover:underline focus-visible:underline",
            urgente ? "text-destructive" : "text-primary"
          )}
        >
          {enlace}
        </a>
      ) : null}
    </li>
  )
}

function CarruselTutoriales({
  tutoriales,
  onAbrir,
}: {
  tutoriales: Tutorial[]
  onAbrir: (tutorial: Tutorial) => void
}) {
  const pista = useRef<HTMLUListElement>(null)
  const [pagina, setPagina] = useState(0)
  const [paginas, setPaginas] = useState(1)

  const medir = useCallback(() => {
    const el = pista.current
    if (!el) return
    const total = Math.max(1, Math.round(el.scrollWidth / el.clientWidth))
    setPaginas(total)
    setPagina(Math.min(total - 1, Math.round(el.scrollLeft / el.clientWidth)))
  }, [])

  useEffect(() => {
    medir()
    window.addEventListener("resize", medir)
    return () => window.removeEventListener("resize", medir)
  }, [medir])

  function irA(indice: number) {
    const el = pista.current
    if (!el) return
    el.scrollTo({ left: indice * el.clientWidth, behavior: "smooth" })
  }

  const flecha =
    "absolute top-[38%] z-10 hidden size-11 -translate-y-1/2 items-center justify-center rounded-lg bg-white/90 text-slate-800 shadow-md outline-none hover:bg-white focus-visible:ring-3 focus-visible:ring-cyan-300 disabled:opacity-0 sm:flex"

  return (
    <div className="relative">
      <button type="button" className={cn(flecha, "-left-2")} onClick={() => irA(pagina - 1)} disabled={pagina === 0}>
        <ChevronLeftIcon className="size-5" aria-hidden="true" />
        <span className="sr-only">Tutoriales anteriores</span>
      </button>
      <button
        type="button"
        className={cn(flecha, "-right-2")}
        onClick={() => irA(pagina + 1)}
        disabled={pagina >= paginas - 1}
      >
        <ChevronRightIcon className="size-5" aria-hidden="true" />
        <span className="sr-only">Más tutoriales</span>
      </button>

      <ul
        ref={pista}
        onScroll={medir}
        className="flex snap-x snap-mandatory gap-5 overflow-x-auto scroll-smooth pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {tutoriales.map((t) => (
          <li
            key={t.titulo}
            className="w-full shrink-0 snap-start sm:w-[calc((100%-1.25rem)/2)] lg:w-[calc((100%-2.5rem)/3)]"
          >
            <button
              type="button"
              onClick={() => onAbrir(t)}
              className="group flex h-full w-full flex-col overflow-hidden rounded-lg bg-white text-left text-slate-900 ring-4 ring-white outline-none focus-visible:ring-cyan-300"
            >
              <MiniaturaTutorial tutorial={t} />
              <span className="flex min-h-24 flex-col gap-1 px-4 py-4">
                <span className="text-lg font-medium">{t.titulo}</span>
                <span className="text-sm text-cyan-700 group-hover:underline">Ver paso a paso</span>
              </span>
            </button>
          </li>
        ))}
      </ul>

      {paginas > 1 ? (
        <div className="mt-6 flex justify-center gap-2" role="group" aria-label="Páginas de tutoriales">
          {Array.from({ length: paginas }, (_, i) => (
            <button
              key={i}
              type="button"
              aria-label={`Ir a la página ${i + 1}`}
              aria-current={i === pagina ? "true" : undefined}
              onClick={() => irA(i)}
              className={cn(
                "size-3 rounded-full outline-none transition-colors focus-visible:ring-3 focus-visible:ring-cyan-300",
                i === pagina ? "bg-white" : "bg-white/40 hover:bg-white/70"
              )}
            />
          ))}
        </div>
      ) : null}
    </div>
  )
}

// Miniatura ilustrada: fondo azul con brillo, globo con la pregunta e icono grande del tema.
function MiniaturaTutorial({ tutorial }: { tutorial: Tutorial }) {
  const { Icono, pregunta } = tutorial
  return (
    <span
      aria-hidden="true"
      className="relative flex aspect-video items-center overflow-hidden bg-linear-to-br from-[#0b2a5b] via-[#0f3d7a] to-[#0b2a5b] px-5"
    >
      <span className="absolute inset-0 bg-[radial-gradient(circle_at_75%_50%,rgb(34_211_238/0.45),transparent_55%)]" />
      <span className="absolute top-1/2 right-[14%] size-36 -translate-y-1/2 rounded-full border-2 border-cyan-300/70 shadow-[0_0_30px_rgb(34_211_238/0.7)]" />
      <span className="relative z-10 max-w-[58%] rounded-2xl bg-white/90 px-3 py-2 text-sm leading-snug font-bold text-[#0b2a5b] shadow-lg">
        {pregunta}
      </span>
      <Icono className="absolute top-1/2 right-[18%] size-20 -translate-y-1/2 text-white drop-shadow-[0_0_12px_rgb(34_211_238/0.9)] transition-transform duration-300 group-hover:scale-110" />
      <span className="absolute bottom-2 left-2 flex items-center gap-1 rounded-full bg-black/40 px-2 py-0.5 text-[11px] font-medium text-white">
        <PlayIcon className="size-3" />
        Tutorial
      </span>
    </span>
  )
}
