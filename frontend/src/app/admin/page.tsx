import type { Metadata } from "next"

import { CrearHorariosCard } from "@/components/admin/crear-horarios-card"
import { ResumenKpis } from "@/components/admin/resumen-kpis"
import { SolicitudesAdminCard } from "@/components/admin/solicitudes-admin-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Panel de administración",
}

// Pantalla 04 · Panel de Administración del prototipo de Figma.
export default function PanelAdminPage() {
  return (
    <>
      <Encabezado
        titulo="Panel de administración y recepción"
        descripcion="Gestiona los horarios de citas, aprueba las solicitudes del personal médico y envía alertas a los pacientes."
      />
      <ResumenKpis />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_440px]">
        <div className="flex flex-col gap-6">
          <SolicitudesAdminCard tipo="CAMBIO_HORARIO" />
          <SolicitudesAdminCard tipo="DERIVACION" />
        </div>
        <CrearHorariosCard />
      </div>
    </>
  )
}
