import { ChartColumnIcon } from "lucide-react"

import { ReportesNav } from "@/components/admin/reportes-nav"
import { HeroSeccion } from "@/components/portal/hero-seccion"

/** Cada reporte es una página propia; el menú de la izquierda cambia de uno a otro. */
export default function ReportesLayout({ children }: LayoutProps<"/admin/reportes">) {
  return (
    <>
      <HeroSeccion
        etiqueta="Administración"
        titulo="Reportes de control"
        descripcion="Elige un reporte en el menú: citas y ausentismo, detalle en PDF, ocupación de horarios o morbilidad y prescripción."
        Icono={ChartColumnIcon}
      />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[256px_minmax(0,1fr)] lg:items-start lg:gap-8 xl:grid-cols-[300px_minmax(0,1fr)]">
        <ReportesNav />
        <div className="flex min-w-0 flex-col gap-6 lg:gap-8">{children}</div>
      </div>
    </>
  )
}
