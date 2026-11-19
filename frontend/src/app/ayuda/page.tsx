import type { Metadata } from "next"
import Link from "next/link"
import { ArrowLeftIcon } from "lucide-react"

import { Logo } from "@/components/brand/logo"
import { CentroAyuda } from "@/components/ayuda/centro-ayuda"

export const metadata: Metadata = {
  title: "Centro de ayuda",
}

// Ayuda pública, enlazada desde el login: se puede consultar sin haber iniciado sesión.
export default function AyudaPublicaPage() {
  return (
    <>
      <header className="sticky top-0 z-40 border-b bg-card/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-[1280px] items-center justify-between gap-4 px-4 sm:px-8">
          <Link href="/login" className="rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
            <Logo sobreClaro className="text-lg" markClassName="size-[30px]" />
          </Link>
          <Link
            href="/login"
            className="flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-sm font-medium text-primary-foreground outline-none hover:bg-[color-mix(in_oklch,var(--primary),black_12%)] focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <ArrowLeftIcon className="size-4" aria-hidden="true" />
            Volver a iniciar sesión
          </Link>
        </div>
      </header>
      <main className="mx-auto flex w-full max-w-[1280px] flex-1 flex-col px-4 py-6 sm:px-8 lg:py-8">
        <CentroAyuda perfil="paciente" />
      </main>
    </>
  )
}
