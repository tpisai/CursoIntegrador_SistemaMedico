import { mutate } from "swr"

import type { Rol, Sesion } from "@/lib/api/types"
import { setSesion } from "./session-store"

// Pantalla inicial de cada rol. El rol ADMISION llega en una siguiente entrega.
export const INICIO_POR_ROL: Partial<Record<Rol, string>> = {
  PACIENTE: "/paciente/agendar",
  DOCTOR: "/doctor",
  ADMINISTRADOR: "/admin",
}

export function guardarSesion(sesion: Sesion) {
  setSesion(sesion)
}

export async function cerrarSesion() {
  setSesion(null)
  // Borra la caché de SWR para que el próximo usuario no vea datos del anterior.
  await mutate(() => true, undefined, { revalidate: false })
}
