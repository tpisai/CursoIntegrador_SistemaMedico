import { request } from "./http"
import type {
  CampaniaAdmin,
  CrearCampaniaRequest,
  CrearHorariosRequest,
  CrearHorariosResponse,
  ResponderSolicitudRequest,
  SolicitudAdmin,
} from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-admin.ts).
export const rutasAdmin = {
  resumen: "/admin/resumen",
  solicitudes: "/admin/solicitudes",
  campanias: "/admin/campanias",
}

export function responderSolicitud(idSolicitud: number, datos: ResponderSolicitudRequest) {
  return request<SolicitudAdmin>("PATCH", `/admin/solicitudes/${idSolicitud}`, datos)
}

export function crearHorarios(datos: CrearHorariosRequest) {
  return request<CrearHorariosResponse>("POST", "/admin/horarios", datos)
}

export function crearCampania(datos: CrearCampaniaRequest) {
  return request<CampaniaAdmin>("POST", "/admin/campanias", datos)
}
