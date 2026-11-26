"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"

import { REPORTES_ADMIN } from "@/lib/admin/reportes"
import { cn } from "@/lib/utils"

/**
 * Menú de reportes de administración. En escritorio es una columna fija a la izquierda;
 * en pantallas angostas, una fila que se desplaza de lado.
 */
export function ReportesNav() {
  const pathname = usePathname()

  return (
    <nav
      aria-label="Reportes"
      className="rounded-2xl bg-card p-2 shadow-[0_6px_24px_rgb(14_60_90/0.06)] ring-1 ring-foreground/[0.07] lg:sticky lg:top-32 lg:p-3"
    >
      <p className="hidden px-3 pt-1 pb-2 text-xs font-semibold tracking-[0.14em] text-muted-foreground uppercase lg:block">
        Reportes
      </p>
      <ul className="flex gap-1.5 overflow-x-auto lg:flex-col lg:overflow-visible">
        {REPORTES_ADMIN.map(({ href, titulo, descripcion, Icono }) => {
          const activo = pathname === href || pathname.startsWith(`${href}/`)
          return (
            <li key={href} className="shrink-0 lg:shrink">
              <Link
                href={href}
                aria-current={activo ? "page" : undefined}
                className={cn(
                  "group flex h-11 items-center gap-2.5 rounded-xl px-3 text-sm outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 lg:h-auto lg:gap-3 lg:py-2.5",
                  activo ? "bg-primary/10 text-primary" : "text-foreground hover:bg-muted"
                )}
              >
                <span
                  className={cn(
                    "flex size-8 shrink-0 items-center justify-center rounded-lg transition-colors lg:size-9",
                    activo ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground group-hover:bg-background"
                  )}
                >
                  <Icono className="size-4 lg:size-[18px]" aria-hidden="true" />
                </span>
                <span className="flex min-w-0 flex-col">
                  <span className="leading-tight font-semibold whitespace-nowrap lg:whitespace-normal">{titulo}</span>
                  <span
                    className={cn(
                      "hidden text-xs leading-tight lg:block",
                      activo ? "text-primary/80" : "text-muted-foreground"
                    )}
                  >
                    {descripcion}
                  </span>
                </span>
              </Link>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
