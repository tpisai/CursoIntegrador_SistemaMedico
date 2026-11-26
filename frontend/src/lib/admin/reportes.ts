import {
  CalendarCheckIcon,
  CalendarRangeIcon,
  FileTextIcon,
  type LucideIcon,
  PillIcon,
} from "lucide-react"

export interface ReporteAdmin {
  href: string
  titulo: string
  // Qué responde el reporte, en una línea, para el menú lateral.
  descripcion: string
  Icono: LucideIcon
}

/**
 * Reportes del panel de administración, en el orden del menú lateral.
 * Para agregar uno: crea su página en `app/admin/reportes/<ruta>/page.tsx` y añádelo aquí.
 */
export const REPORTES_ADMIN: ReporteAdmin[] = [
  {
    href: "/admin/reportes/citas",
    titulo: "Citas y ausentismo",
    descripcion: "Por especialidad y doctor",
    Icono: CalendarCheckIcon,
  },
  {
    href: "/admin/reportes/integral",
    titulo: "Reporte integral de citas",
    descripcion: "Detalle de cada cita en PDF",
    Icono: FileTextIcon,
  },
  {
    href: "/admin/reportes/horarios",
    titulo: "Ocupación de horarios",
    descripcion: "Turnos ofertados y ocupados",
    Icono: CalendarRangeIcon,
  },
  {
    href: "/admin/reportes/prescripcion",
    titulo: "Morbilidad y prescripción",
    descripcion: "Diagnósticos y medicamentos",
    Icono: PillIcon,
  },
]

/** Reporte que se abre al entrar a /admin/reportes. */
export const REPORTE_INICIAL = REPORTES_ADMIN[0].href
