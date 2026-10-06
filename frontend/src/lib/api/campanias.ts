import { request, requestArchivo } from "./http"
import type { Inscripcion } from "./types"

export const RUTA_CAMPANIAS = "/campanias"

export function inscribirse(idCampania: number) {
  return request<Inscripcion>("POST", `/campanias/${idCampania}/inscripcion`)
}

export function descargarComprobante(idInscripcion: number) {
  return requestArchivo(`/campanias/inscripciones/${idInscripcion}/comprobante`)
}
