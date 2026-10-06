import type { Metadata } from "next"
import { TriangleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { AgendaDelDia } from "@/components/doctor/agenda-del-dia"
import { CambioFechaCard } from "@/components/doctor/cambio-fecha-card"
import { DerivacionCard } from "@/components/doctor/derivacion-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Panel del doctor",
}

// Pantalla 03 · Panel del Doctor del prototipo de Figma.
export default function PanelDoctorPage() {
  return (
    <>
      <Encabezado
        titulo="Panel del doctor"
        descripcion="Revisa tus citas pendientes asignadas y coordina cambios de fecha o derivaciones con administración."
      />
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="flex flex-col gap-6">
          <AgendaDelDia />
          <Alert variant="warning">
            <TriangleAlertIcon />
            <AlertDescription>
              Los cambios de fecha y derivaciones requieren aprobación de administración antes de notificar al
              paciente.
            </AlertDescription>
          </Alert>
        </div>
        <div className="flex flex-col gap-6">
          <CambioFechaCard />
          <DerivacionCard />
        </div>
      </div>
    </>
  )
}
