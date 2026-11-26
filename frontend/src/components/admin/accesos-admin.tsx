import Link from "next/link"
import {
  CalendarPlusIcon,
  ChartColumnIcon,
  GalleryHorizontalEndIcon,
  type LucideIcon,
  MegaphoneIcon,
  TicketCheckIcon,
} from "lucide-react"

import { cn } from "@/lib/utils"

const ACCESOS: { href: string; titulo: string; texto: string; Icono: LucideIcon; tono: string }[] = [
  {
    href: "#recepcion",
    titulo: "Validar ticket",
    texto: "Registra la llegada del paciente",
    Icono: TicketCheckIcon,
    tono: "from-cyan-600 to-teal-500",
  },
  {
    href: "#horarios",
    titulo: "Crear horarios",
    texto: "Abre turnos para las citas",
    Icono: CalendarPlusIcon,
    tono: "from-blue-700 to-sky-500",
  },
  {
    href: "/admin/comunicacion",
    titulo: "Campañas",
    texto: "Publica y revisa inscritos",
    Icono: MegaphoneIcon,
    tono: "from-violet-700 to-fuchsia-500",
  },
  {
    href: "/admin/anuncios",
    titulo: "Carrusel",
    texto: "Banners de eventos",
    Icono: GalleryHorizontalEndIcon,
    tono: "from-rose-600 to-orange-400",
  },
  {
    href: "/admin/reportes",
    titulo: "Reportes",
    texto: "Citas, ausentismo y ocupación",
    Icono: ChartColumnIcon,
    tono: "from-emerald-700 to-lime-500",
  },
]

/** Fila de accesos rápidos del panel de administración. */
export function AccesosAdmin() {
  return (
    <nav aria-label="Accesos rápidos">
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 lg:gap-4">
        {ACCESOS.map(({ href, titulo, texto, Icono, tono }) => (
          <li key={titulo}>
            <Link
              href={href}
              className="group flex h-full items-center gap-3 rounded-2xl bg-card p-4 shadow-[0_6px_24px_rgb(14_60_90/0.06)] ring-1 ring-foreground/[0.07] outline-none transition-[translate,box-shadow] hover:-translate-y-0.5 hover:shadow-[0_12px_30px_rgb(14_60_90/0.12)] focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <span
                className={cn(
                  "flex size-11 shrink-0 items-center justify-center rounded-xl bg-linear-to-br text-white shadow-md transition-transform group-hover:scale-105",
                  tono
                )}
              >
                <Icono className="size-5" aria-hidden="true" />
              </span>
              <span className="flex min-w-0 flex-col">
                <span className="text-sm leading-tight font-bold">{titulo}</span>
                <span className="text-xs leading-tight text-muted-foreground">{texto}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  )
}
