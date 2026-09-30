import { request } from "./http"
import type { CambioFechaRequest, DerivacionRequest, Solicitud } from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-doctor.ts).
export const rutasDoctor = {
  citasDelDia: (fecha: string) => `/doctor/citas?fecha=${fecha}`,
  citasProximas: "/doctor/citas/proximas",
  horariosLibres: "/doctor/horarios-libres",
  solicitudes: "/doctor/solicitudes",
}

export function solicitarCambioFecha(datos: CambioFechaRequest) {
  return request<Solicitud>("POST", "/doctor/solicitudes/cambio-fecha", datos)
}

export function solicitarDerivacion(datos: DerivacionRequest) {
  return request<Solicitud>("POST", "/doctor/solicitudes/derivacion", datos)
}
