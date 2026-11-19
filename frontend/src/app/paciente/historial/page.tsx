import type { Metadata } from "next"

import { HistorialCard } from "@/components/citas/historial-card"
import { ReferenciasDerivacionCard } from "@/components/citas/referencias-derivacion-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Historial médico",
}

export default function HistorialPage() {
  return (
    <>
      <Encabezado
        titulo="Historial médico"
        descripcion="Tus atenciones anteriores y los documentos que el personal de salud cargó para ti."
      />
      <div className="flex max-w-3xl flex-col gap-6">
        <ReferenciasDerivacionCard />
        <HistorialCard />
      </div>
    </>
  )
}
