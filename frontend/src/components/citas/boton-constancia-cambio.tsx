"use client"

import { useState } from "react"
import { toast } from "sonner"
import { FileDownIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { mensajeDeError } from "@/lib/api/http"
import { descargarConstanciaCambio } from "@/lib/api/solicitudes"
import type { EstadoSolicitud, TipoSolicitud } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"

/** RF-17: la constancia del cambio de fecha solo existe después de la aprobación de administración. */
export function tieneConstanciaCambio(tipo: TipoSolicitud, estado: EstadoSolicitud) {
  return tipo === "CAMBIO_HORARIO" && (estado === "APROBADA" || estado === "RESUELTA")
}

export function BotonConstanciaCambio({ idSolicitud, size = "sm" }: { idSolicitud: number; size?: "sm" | "xs" }) {
  const [descargando, setDescargando] = useState(false)

  async function descargar() {
    setDescargando(true)
    try {
      const archivo = await descargarConstanciaCambio(idSolicitud)
      guardarArchivo(archivo, `constancia-cambio-${idSolicitud}.pdf`)
    } catch (error) {
      toast.error(mensajeDeError(error))
    } finally {
      setDescargando(false)
    }
  }

  return (
    <Button variant="outline" size={size} disabled={descargando} onClick={descargar}>
      {descargando ? <Spinner data-icon="inline-start" /> : <FileDownIcon data-icon="inline-start" />}
      Constancia
    </Button>
  )
}
