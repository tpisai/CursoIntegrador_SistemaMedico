import { request } from "./http"

export const RUTA_NOTIFICACIONES = "/notificaciones"

export function marcarNotificacionLeida(idNotificacion: number) {
  return request<void>("PATCH", `/notificaciones/${idNotificacion}/leida`)
}

export function marcarTodasLeidas() {
  return request<void>("PATCH", "/notificaciones/leidas")
}
