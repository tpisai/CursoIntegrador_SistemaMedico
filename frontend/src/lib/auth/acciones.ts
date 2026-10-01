import { mutate } from "swr"

import type { Rol, Sesion } from "@/lib/api/types"
import { setSesion } from "./session-store"

// Pantalla inicial de cada rol. Admisión y administración llegan en las siguientes entregas.
export const INICIO_POR_ROL: Partial<Record<Rol, string>> = {
  PACIENTE: "/paciente/agendar",
  DOCTOR: "/doctor",
}

export function guardarSesion(sesion: Sesion) {
  setSesion(sesion)
}

export async function cerrarSesion() {
  setSesion(null)
  // Borra la caché de SWR para que el próximo usuario no vea datos del anterior.
  await mutate(() => true, undefined, { revalidate: false })
}
