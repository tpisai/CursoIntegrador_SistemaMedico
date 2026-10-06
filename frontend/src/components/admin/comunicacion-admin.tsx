"use client"

import { useState } from "react"
import { PlusIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { CampaniasAdminCard } from "@/components/admin/campanias-admin-card"
import { NuevaCampaniaDialog } from "@/components/admin/nueva-campania-dialog"
import { Encabezado } from "@/components/portal/encabezado"

export function ComunicacionAdmin() {
  const [nueva, setNueva] = useState(false)

  return (
    <>
      <Encabezado
        titulo="Comunicación"
        descripcion="Publica campañas médicas que los pacientes verán en el portal y a las que podrán inscribirse."
      >
        <Button size="xl" onClick={() => setNueva(true)}>
          <PlusIcon data-icon="inline-start" />
          Nueva campaña
        </Button>
      </Encabezado>
      <CampaniasAdminCard />
      <NuevaCampaniaDialog open={nueva} onOpenChange={setNueva} />
    </>
  )
}
