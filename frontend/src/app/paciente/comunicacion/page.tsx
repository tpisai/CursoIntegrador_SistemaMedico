import type { Metadata } from "next"
import { MegaphoneIcon } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Encabezado } from "@/components/portal/encabezado"

export const metadata: Metadata = {
  title: "Comunicación",
}

// Pantalla 05 del prototipo (campañas y anuncios): se implementa en la siguiente entrega.
export default function ComunicacionPage() {
  return (
    <>
      <Encabezado
        titulo="Comunicación y campañas"
        descripcion="Campañas médicas y anuncios del Centro de Salud Miguel Grau."
      />
      <Card>
        <CardContent>
          <Empty className="py-12">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <MegaphoneIcon />
              </EmptyMedia>
              <EmptyTitle>Muy pronto</EmptyTitle>
              <EmptyDescription>
                Aquí verás las campañas de salud del centro y podrás inscribirte en ellas.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        </CardContent>
      </Card>
    </>
  )
}
