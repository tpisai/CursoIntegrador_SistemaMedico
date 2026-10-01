import { Badge } from "@/components/ui/badge"
import type { EstadoCita, EstadoSolicitud } from "@/lib/api/types"

type Variante = "success" | "warning" | "destructive" | "outline"

const ESTADOS: Record<EstadoCita, { texto: string; variant: Variante }> = {
  CONFIRMADA: { texto: "Confirmada", variant: "success" },
  RESERVADA: { texto: "Pendiente", variant: "warning" },
  ATENDIDA: { texto: "Asistió", variant: "success" },
  NO_ASISTIO: { texto: "No asistió", variant: "destructive" },
  CANCELADA: { texto: "Cancelada", variant: "outline" },
}

/** vista "doctor" usa los textos del panel del doctor ("En espera" en vez de "Pendiente"). */
export function EstadoCitaBadge({ estado, vista = "paciente" }: { estado: EstadoCita; vista?: "paciente" | "doctor" }) {
  const { texto, variant } = ESTADOS[estado]
  return <Badge variant={variant}>{vista === "doctor" && estado === "RESERVADA" ? "En espera" : texto}</Badge>
}

const ESTADOS_SOLICITUD: Record<EstadoSolicitud, { texto: string; variant: Variante }> = {
  PENDIENTE: { texto: "Pendiente", variant: "warning" },
  EN_REVISION: { texto: "En revisión", variant: "outline" },
  APROBADA: { texto: "Aprobada", variant: "success" },
  RECHAZADA: { texto: "Rechazada", variant: "destructive" },
}

export function EstadoSolicitudBadge({ estado }: { estado: EstadoSolicitud }) {
  const { texto, variant } = ESTADOS_SOLICITUD[estado]
  return <Badge variant={variant}>{texto}</Badge>
}
