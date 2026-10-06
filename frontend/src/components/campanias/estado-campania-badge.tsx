import { Badge } from "@/components/ui/badge"
import type { EstadoCampania } from "@/lib/api/types"

const ESTADOS: Record<EstadoCampania, { texto: string; variant: "success" | "warning" | "destructive" | "outline" }> = {
  ACTIVA: { texto: "En curso", variant: "success" },
  PLANIFICADA: { texto: "Próxima", variant: "warning" },
  FINALIZADA: { texto: "Finalizada", variant: "outline" },
  CANCELADA: { texto: "Cancelada", variant: "destructive" },
}

export function EstadoCampaniaBadge({ estado }: { estado: EstadoCampania }) {
  const { texto, variant } = ESTADOS[estado]
  return <Badge variant={variant}>{texto}</Badge>
}
