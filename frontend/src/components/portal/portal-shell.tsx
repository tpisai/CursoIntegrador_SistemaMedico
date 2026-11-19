"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

import Link from "next/link"

import { Logo } from "@/components/brand/logo"
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
  const ayuda = enlaces.find((e) => e.href.endsWith("/ayuda"))?.href
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
      <main className="mx-auto flex w-full max-w-[1280px] flex-1 flex-col gap-6 px-4 py-6 sm:px-8 lg:gap-8 lg:py-8">
        {children}
      </main>
      <footer className="mt-6 bg-navy text-navy-foreground">
        <div className="mx-auto flex w-full max-w-[1280px] flex-col gap-4 px-4 py-8 text-sm sm:flex-row sm:items-center sm:justify-between sm:gap-6 sm:px-8">
          <div className="flex items-center gap-3">
            <Logo className="text-lg" markClassName="size-8" />
            <span className="opacity-80">Centro de Salud Miguel Grau, Chaclacayo</span>
          </div>
          <div className="flex flex-col gap-1 opacity-85 sm:items-end">
            {ayuda ? (
              <Link href={ayuda} className="font-semibold text-cyan-300 outline-none hover:underline focus-visible:underline">
                Centro de ayuda
              </Link>
            ) : null}
            {rol === "PACIENTE" ? <span>En una emergencia no reserves cita: acude al área de Emergencia.</span> : null}
          </div>
        </div>
      </footer>
    </>
  )
}
