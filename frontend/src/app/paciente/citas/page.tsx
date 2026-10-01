import type { Metadata } from "next"

import { MisCitasCard } from "@/components/citas/mis-citas-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Mis citas",
}

export default function MisCitasPage() {
  return (
    <>
      <Encabezado
        titulo="Mis citas"
        descripcion="Revisa tus citas activas, muestra tu ticket en recepción o cancela si no podrás asistir."
      />
      <MisCitasCard mostrarAgendar />
    </>
  )
}
