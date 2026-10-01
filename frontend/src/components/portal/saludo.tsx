"use client"

import { useSesion } from "@/hooks/use-session"
import { primerNombre } from "@/lib/format"

export function Saludo() {
  const sesion = useSesion()
  return <>Hola, {sesion ? primerNombre(sesion.usuario.nombres) : ""}</>
}
