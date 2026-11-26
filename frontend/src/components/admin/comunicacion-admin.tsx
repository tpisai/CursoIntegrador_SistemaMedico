"use client"

import { useState } from "react"
import Link from "next/link"
import { GalleryHorizontalEndIcon, MegaphoneIcon, PlusIcon, TicketIcon, UsersIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { CampaniasAdminCard } from "@/components/admin/campanias-admin-card"
import { CarruselResumenCard } from "@/components/admin/carrusel-resumen-card"
import { NuevaCampaniaDialog } from "@/components/admin/nueva-campania-dialog"
import { DatoHero, HeroSeccion } from "@/components/portal/hero-seccion"
import { useCampaniasAdmin } from "@/hooks/use-admin"

export function ComunicacionAdmin() {
  const [nueva, setNueva] = useState(false)
  const { data: campanias } = useCampaniasAdmin()

  const vigentes = campanias?.filter((c) => c.estado === "ACTIVA" || c.estado === "PLANIFICADA")
  const inscritos = campanias?.reduce((total, c) => total + c.inscritos, 0)
  const cuposLibres = vigentes?.reduce((total, c) => total + c.cuposDisponibles, 0)

  return (
    <>
      <HeroSeccion
        etiqueta="Comunicación"
        titulo="Campañas médicas"
        descripcion="Publica campañas que los pacientes verán en su portal y a las que podrán inscribirse. Al publicarla, todos reciben una notificación."
        Icono={MegaphoneIcon}
        pie={
          <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <DatoHero Icono={MegaphoneIcon} valor={vigentes?.length ?? "…"} texto="Campañas vigentes" />
            <DatoHero Icono={UsersIcon} valor={inscritos ?? "…"} texto="Pacientes inscritos" />
            <DatoHero Icono={TicketIcon} valor={cuposLibres ?? "…"} texto="Cupos libres" />
            <DatoHero Icono={GalleryHorizontalEndIcon} valor={campanias?.length ?? "…"} texto="Campañas publicadas" />
          </ul>
        }
      >
        <Button size="xl" variant="lime" className="rounded-full" onClick={() => setNueva(true)}>
          <PlusIcon data-icon="inline-start" />
          Nueva campaña
        </Button>
        <Button
          asChild
          size="xl"
          variant="outline"
          className="rounded-full border-white/40 bg-white/10 text-white hover:bg-white/20 hover:text-white"
        >
          <Link href="/admin/anuncios">
            <GalleryHorizontalEndIcon data-icon="inline-start" />
            Carrusel de eventos
          </Link>
        </Button>
      </HeroSeccion>

      <div className="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px] xl:gap-8">
        <CampaniasAdminCard />
        <CarruselResumenCard />
      </div>
      <NuevaCampaniaDialog open={nueva} onOpenChange={setNueva} />
    </>
  )
}
