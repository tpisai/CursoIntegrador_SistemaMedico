import type { Metadata } from "next"
import Link from "next/link"
import {
  ClipboardListIcon,
  GalleryHorizontalEndIcon,
  LayoutDashboardIcon,
  type LucideIcon,
  TicketCheckIcon,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { AccesosAdmin } from "@/components/admin/accesos-admin"
import { CarruselResumenCard } from "@/components/admin/carrusel-resumen-card"
import { CrearHorariosCard } from "@/components/admin/crear-horarios-card"
import { FechaHoy, ResumenKpis } from "@/components/admin/resumen-kpis"
import { SolicitudesAdminCard } from "@/components/admin/solicitudes-admin-card"
import { ValidarTicketCard } from "@/components/admin/validar-ticket-card"
import { HeroSeccion } from "@/components/portal/hero-seccion"
import { Saludo } from "@/components/portal/saludo"

export const metadata: Metadata = {
  title: "Panel de administración",
}

function TituloSeccion({ id, Icono, titulo, texto }: { id: string; Icono: LucideIcon; titulo: string; texto: string }) {
  return (
    <div className="flex items-center gap-3">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-secondary text-primary">
        <Icono className="size-5" aria-hidden="true" />
      </span>
      <div className="flex flex-col">
        <h2 id={id} className="text-xl font-bold tracking-tight">
          {titulo}
        </h2>
        <p className="text-sm text-muted-foreground">{texto}</p>
      </div>
    </div>
  )
}

// Pantalla 04 · Panel de Administración del prototipo de Figma.
export default function PanelAdminPage() {
  return (
    <>
      <HeroSeccion
        etiqueta="Panel de administración y recepción"
        titulo={<Saludo />}
        descripcion={
          <>
            Hoy es <FechaHoy />. Valida los tickets en recepción, gestiona los horarios, aprueba las solicitudes del
            personal médico y mantén informados a los pacientes.
          </>
        }
        Icono={LayoutDashboardIcon}
        pie={<ResumenKpis />}
      >
        <Button asChild size="xl" variant="lime" className="rounded-full">
          <Link href="#recepcion">
            <TicketCheckIcon data-icon="inline-start" />
            Validar ticket
          </Link>
        </Button>
        <Button
          asChild
          size="xl"
          variant="outline"
          className="rounded-full border-white/40 bg-white/10 text-white hover:bg-white/20 hover:text-white"
        >
          <Link href="/admin/anuncios">
            <GalleryHorizontalEndIcon data-icon="inline-start" />
            Publicar banner
          </Link>
        </Button>
      </HeroSeccion>

      <AccesosAdmin />

      <div className="grid grid-cols-1 items-start gap-8 xl:grid-cols-[minmax(0,1fr)_440px]">
        <section aria-labelledby="titulo-solicitudes" className="flex flex-col gap-5">
          <TituloSeccion
            id="titulo-solicitudes"
            Icono={ClipboardListIcon}
            titulo="Solicitudes del personal médico"
            texto="Cambios de fecha y derivaciones que esperan tu respuesta."
          />
          <SolicitudesAdminCard tipo="CAMBIO_HORARIO" />
          <SolicitudesAdminCard tipo="DERIVACION" />
        </section>
        <section aria-labelledby="titulo-recepcion" className="flex flex-col gap-5">
          <TituloSeccion
            id="titulo-recepcion"
            Icono={TicketCheckIcon}
            titulo="Recepción y agenda"
            texto="Llegada de pacientes y turnos disponibles."
          />
          <div id="recepcion" className="scroll-mt-32">
            <ValidarTicketCard />
          </div>
          <div id="horarios" className="scroll-mt-32">
            <CrearHorariosCard />
          </div>
          <CarruselResumenCard />
        </section>
      </div>
    </>
  )
}
