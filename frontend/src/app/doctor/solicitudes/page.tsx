import type { Metadata } from "next"
import { TriangleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { CambioFechaCard } from "@/components/doctor/cambio-fecha-card"
import { DerivacionCard } from "@/components/doctor/derivacion-card"
import { SolicitudesCard } from "@/components/doctor/solicitudes-card"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Solicitudes",
}

// RF-15 y RF-16: el doctor pide el cambio o la derivación y sigue la respuesta en la misma pantalla.
export default function SolicitudesDoctorPage() {
  return (
    <>
      <Encabezado
        titulo="Solicitudes"
        descripcion="Pide un cambio de fecha o deriva a un paciente, y sigue la respuesta de administración."
      />
      <Alert variant="warning">
        <TriangleAlertIcon />
        <AlertDescription>
          Los cambios de fecha y las derivaciones requieren aprobación de administración antes de notificar al
          paciente.
        </AlertDescription>
      </Alert>
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
        <CambioFechaCard />
        <DerivacionCard />
      </div>
      <SolicitudesCard />
    </>
  )
}
