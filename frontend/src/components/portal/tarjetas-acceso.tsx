import Link from "next/link"
import { ArrowRightIcon, LifeBuoyIcon, type LucideIcon } from "lucide-react"

import { LogoMark } from "@/components/brand/logo"
import { cn } from "@/lib/utils"

export type ColorAcceso = "azul" | "verde" | "amarillo" | "celeste" | "violeta" | "rosa"

export type Acceso = {
  href: string
  titulo: string
  texto: string
  boton: string
  Icono: LucideIcon
  color: ColorAcceso
}

// Icono y botón de cada tarjeta. Los tonos claros llevan texto oscuro para mantener el contraste.
const COLORES: Record<ColorAcceso, { icono: string; boton: string }> = {
  azul: { icono: "text-blue-600", boton: "bg-blue-600 text-white hover:bg-blue-700" },
  verde: { icono: "text-emerald-700", boton: "bg-emerald-700 text-white hover:bg-emerald-800" },
  amarillo: { icono: "text-amber-500", boton: "bg-amber-400 text-amber-950 hover:bg-amber-500" },
  celeste: { icono: "text-cyan-500", boton: "bg-cyan-400 text-cyan-950 hover:bg-cyan-500" },
  violeta: { icono: "text-violet-600", boton: "bg-violet-600 text-white hover:bg-violet-700" },
  rosa: { icono: "text-rose-500", boton: "bg-rose-500 text-white hover:bg-rose-600" },
}

/** Título centrado con el isotipo, como cabecera de los portales. */
export function TituloPortal({ titulo, subtitulo }: { titulo: string; subtitulo?: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 pt-2 text-center sm:flex-row sm:justify-center sm:gap-5 sm:text-left">
      <LogoMark sobreClaro className="size-14 drop-shadow-md sm:size-16" />
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl leading-tight font-semibold text-primary sm:text-[32px]">{titulo}</h1>
        {subtitulo ? <p className="text-muted-foreground">{subtitulo}</p> : null}
      </div>
    </div>
  )
}

/** Cuadrícula de accesos: icono grande, título, descripción y botón de color a todo el ancho. */
export function TarjetasAcceso({ accesos }: { accesos: Acceso[] }) {
  return (
    <nav aria-label="Accesos rápidos">
      <ul className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 lg:gap-7">
        {accesos.map(({ href, titulo, texto, boton, Icono, color }) => (
          <li
            key={titulo}
            className="flex flex-col items-center gap-3 rounded-2xl bg-card p-5 pt-7 text-center shadow-[0_6px_24px_rgb(14_60_90/0.07)] ring-1 ring-foreground/[0.07] transition-transform duration-200 hover:-translate-y-1"
          >
            <Icono className={cn("size-12", COLORES[color].icono)} strokeWidth={2.2} aria-hidden="true" />
            <h2 className="text-xl font-semibold">{titulo}</h2>
            <p className="flex-1 text-[15px] leading-relaxed text-muted-foreground">{texto}</p>
            <Link
              href={href}
              className={cn(
                "mt-2 flex h-11 w-full items-center justify-center rounded-lg text-[15px] font-medium outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50",
                COLORES[color].boton
              )}
            >
              {boton}
              <span className="sr-only">: {titulo}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  )
}

/** Banda inferior "¿Necesitas ayuda?" que lleva al centro de ayuda del portal. */
export function BannerAyuda({ href }: { href: string }) {
  return (
    <section
      aria-labelledby="banner-ayuda"
      className="flex flex-col items-center gap-5 rounded-2xl bg-card p-6 text-center shadow-[0_6px_24px_rgb(14_60_90/0.07)] ring-1 ring-foreground/[0.07] sm:flex-row sm:text-left"
    >
      <span className="flex size-20 shrink-0 items-center justify-center rounded-2xl bg-navy text-navy-foreground">
        <LifeBuoyIcon className="size-10" aria-hidden="true" />
      </span>
      <div className="flex flex-1 flex-col gap-1">
        <h2 id="banner-ayuda" className="text-2xl font-medium text-muted-foreground">
          ¿Necesitas ayuda?
        </h2>
        <p>Mira los tutoriales y las guías paso a paso para usar el portal sin complicaciones.</p>
      </div>
      <Link
        href={href}
        className="flex h-11 items-center gap-2 rounded-full bg-navy px-6 font-medium text-navy-foreground outline-none hover:bg-[color-mix(in_oklch,var(--navy),white_10%)] focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        Ir al centro de ayuda
        <ArrowRightIcon className="size-4" aria-hidden="true" />
      </Link>
    </section>
  )
}
