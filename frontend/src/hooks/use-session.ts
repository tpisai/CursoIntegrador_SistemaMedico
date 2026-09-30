"use client"

import { useSyncExternalStore } from "react"

import { getSesion, subscribeSesion } from "@/lib/auth/session-store"
import type { Sesion } from "@/lib/api/types"

/**
 * Sesión actual. `undefined` mientras se hidrata la página (aún no se leyó localStorage),
 * `null` si no hay sesión. Así los guardias no redirigen antes de tiempo.
 */
export function useSesion(): Sesion | null | undefined {
  return useSyncExternalStore(subscribeSesion, getSesion, () => undefined)
}
