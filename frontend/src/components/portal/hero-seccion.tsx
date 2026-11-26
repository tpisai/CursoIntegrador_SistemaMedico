import type { LucideIcon } from "lucide-react"

import { cn } from "@/lib/utils"

/**
 * Cabecera con degradado para las secciones principales (historial, campañas, panel de
 * administración). Las acciones van a la derecha en escritorio y debajo en móvil.
 */
export function HeroSeccion({
  titulo,
  descripcion,
  etiqueta,
  Icono,
  children,
  pie,
  className,
}: {
  titulo: React.ReactNode
  descripcion: React.ReactNode
  // Texto pequeño sobre el título, p. ej. "Portal del paciente".
  etiqueta?: string
  Icono?: LucideIcon
  // Botones de acción.
  children?: React.ReactNode
  // Fila inferior (indicadores, chips…).
  pie?: React.ReactNode
  className?: string
}) {
  return (
    <section
      className={cn(
        "relative overflow-hidden rounded-3xl bg-linear-to-br from-brand-strong via-primary to-brand px-6 py-7 text-white shadow-[0_14px_40px_rgb(14_60_90/0.20)] sm:px-8 lg:px-10 lg:py-9",
        className
      )}
    >
      <span aria-hidden="true" className="absolute -top-24 -right-16 size-72 rounded-full bg-white/10" />
      <span aria-hidden="true" className="absolute right-40 -bottom-28 size-56 rounded-full bg-white/[0.06]" />
      <span aria-hidden="true" className="absolute top-6 right-[38%] hidden text-4xl font-thin opacity-30 lg:block">
        +
      </span>

      <div className="relative flex flex-col gap-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            {Icono ? (
              <span className="hidden size-14 shrink-0 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/25 backdrop-blur sm:flex">
                <Icono className="size-7" aria-hidden="true" />
              </span>
            ) : null}
            <div className="flex flex-col gap-1.5">
              {etiqueta ? (
                <span className="text-xs font-semibold tracking-[0.14em] text-cyan-100 uppercase">{etiqueta}</span>
              ) : null}
              <h1 className="text-[26px] leading-tight font-bold tracking-tight text-balance lg:text-[34px]">{titulo}</h1>
              <p className="max-w-[68ch] text-[15px] leading-relaxed text-white/85">{descripcion}</p>
            </div>
          </div>
          {children ? <div className="flex shrink-0 flex-wrap gap-2.5">{children}</div> : null}
        </div>
        {pie}
      </div>
    </section>
  )
}

/** Indicador translúcido para la fila inferior del HeroSeccion. */
export function DatoHero({ valor, texto, Icono }: { valor: React.ReactNode; texto: string; Icono: LucideIcon }) {
  return (
    <li className="flex items-center gap-3 rounded-2xl bg-white/12 px-4 py-3 ring-1 ring-white/20 backdrop-blur">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/15">
        <Icono className="size-5" aria-hidden="true" />
      </span>
      <span className="flex min-w-0 flex-col">
        <span className="text-xl leading-tight font-bold tabular-nums">{valor}</span>
        <span className="text-xs leading-tight text-white/80">{texto}</span>
      </span>
    </li>
  )
}
