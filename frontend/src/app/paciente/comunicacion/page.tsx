import type { Metadata } from "next"

import { CampaniasPaciente } from "@/components/campanias/campanias-paciente"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Comunicación",
}

// Pantalla 05 · Comunicación y Campañas del prototipo de Figma.
export default function ComunicacionPage() {
  return (
    <>
      <Encabezado
        titulo="Campañas médicas"
        descripcion="Campañas gratuitas del Centro de Salud Miguel Grau para la comunidad de Chaclacayo."
      />
      <CampaniasPaciente />
    </>
  )
}
