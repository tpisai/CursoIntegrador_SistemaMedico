import { request, requestArchivo } from "./http"
import type { ResolucionCambio, ResolverCambioRequest } from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-solicitudes.ts).
export const rutasSolicitudes = {
  pendientes: "/solicitudes/pendientes",
  mias: "/solicitudes/mias",
}

export function resolverCambio(idSolicitud: number, datos: ResolverCambioRequest) {
  return request<ResolucionCambio>("POST", `/solicitudes/${idSolicitud}/resolver`, datos)
}

/** Constancia en PDF del cambio de fecha: la descargan paciente, doctor y administración. */
export function descargarConstanciaCambio(idSolicitud: number) {
  return requestArchivo(`/solicitudes/${idSolicitud}/constancia`)
}
