"use client"

import useSWR from "swr"

import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { Notificacion } from "@/lib/api/types"

export function useNotificaciones() {
  return useSWR<Notificacion[]>(RUTA_NOTIFICACIONES, { refreshInterval: 30_000, revalidateOnFocus: true })
}
