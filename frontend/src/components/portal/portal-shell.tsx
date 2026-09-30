"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

import { Spinner } from "@/components/ui/spinner"
import { type EnlacePortal, PortalNavbar } from "@/components/portal/portal-navbar"
import { useSesion } from "@/hooks/use-session"
import type { Rol } from "@/lib/api/types"

/** Estructura común de los portales: exige sesión con el rol indicado y muestra la barra. */
export function PortalShell({
  rol,
  enlaces,
  etiquetaRol,
  children,
}: {
  rol: Rol
  enlaces: EnlacePortal[]
  etiquetaRol?: string
  children: React.ReactNode
}) {
  const sesion = useSesion()
  const router = useRouter()
  // undefined = todavía hidratando; null = sin sesión.
  const autorizado = sesion?.usuario.rol === rol
  const debeSalir = sesion === null || (sesion !== undefined && !autorizado)

  useEffect(() => {
    if (debeSalir) router.replace("/login")
  }, [debeSalir, router])

  if (!sesion || !autorizado) {
    return (
      <div className="flex flex-1 items-center justify-center" role="status">
        <Spinner className="size-6 text-primary" />
        <span className="sr-only">Cargando tu portal…</span>
      </div>
    )
  }

  return (
    <>
      <PortalNavbar usuario={sesion.usuario} enlaces={enlaces} etiquetaRol={etiquetaRol} />
      <main className="mx-auto flex w-full max-w-[1440px] flex-1 flex-col gap-6 px-4 py-6 sm:px-8 lg:py-8">
        {children}
      </main>
    </>
  )
}
