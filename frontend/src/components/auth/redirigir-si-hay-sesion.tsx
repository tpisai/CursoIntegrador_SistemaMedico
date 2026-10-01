"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

import { useSesion } from "@/hooks/use-session"
import { INICIO_POR_ROL } from "@/lib/auth/acciones"

/** Si el usuario ya inició sesión, lo lleva directo a su panel. */
export function RedirigirSiHaySesion() {
  const sesion = useSesion()
  const router = useRouter()
  const destino = sesion ? INICIO_POR_ROL[sesion.usuario.rol] : undefined

  useEffect(() => {
    if (destino) router.replace(destino)
  }, [destino, router])

  return null
}
