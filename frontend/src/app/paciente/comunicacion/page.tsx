import type { Metadata } from "next"

import { CarruselEventos } from "@/components/anuncios/carrusel-eventos"
import { CampaniasPaciente } from "@/components/campanias/campanias-paciente"
import {
  BannerAdultoMayor,
  BeneficiosCampanias,
  CalendarioSalud,
  ConsejosPrevencion,
  PreguntasCampanias,
} from "@/components/campanias/contenido-campanias"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Campañas y eventos",
}

// Pantalla 05 · Comunicación y Campañas del prototipo de Figma.
export default function ComunicacionPage() {
  return (
    <>
      <Encabezado
        titulo="Campañas y eventos de salud"
        descripcion="Novedades, eventos y campañas gratuitas del Centro de Salud Miguel Grau para la comunidad de Chaclacayo."
      />
      <CarruselEventos />
      <BeneficiosCampanias />
      <CampaniasPaciente />
      <BannerAdultoMayor />
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_380px] lg:gap-8">
        <ConsejosPrevencion />
        <CalendarioSalud />
      </div>
      <PreguntasCampanias />
    </>
  )
}
