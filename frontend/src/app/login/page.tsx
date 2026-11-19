import type { Metadata } from "next"
import Link from "next/link"
import { CircleHelpIcon, HeartPulseIcon, SirenIcon } from "lucide-react"

import { Logo } from "@/components/brand/logo"
import { LoginCarrusel } from "@/components/auth/login-carrusel"
import { LoginForm } from "@/components/auth/login-form"
import { RedirigirSiHaySesion } from "@/components/auth/redirigir-si-hay-sesion"

export const metadata: Metadata = {
  title: "Iniciar sesión",
}

export default function LoginPage() {
  return (
    <main className="flex min-h-svh flex-1 flex-col bg-card lg:grid lg:grid-cols-[minmax(0,44%)_1fr]">
      <RedirigirSiHaySesion />

      {/* Panel de marca con carrusel */}
      <section className="relative flex flex-col gap-6 overflow-hidden bg-brand px-6 py-6 text-brand-foreground lg:px-14 lg:py-12">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -top-24 -right-24 size-72 rounded-full bg-white/10 blur-2xl"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -bottom-32 -left-20 size-80 rounded-full bg-brand-strong/40 blur-3xl"
        />
        <Logo className="relative text-2xl lg:text-4xl" markClassName="size-10 lg:size-14" />
        <div className="relative hidden flex-1 items-center justify-center lg:flex">
          <LoginCarrusel />
        </div>
        <p className="relative font-medium opacity-90 lg:hidden">Centro de Salud Miguel Grau · Chaclacayo</p>
      </section>

      {/* Formulario */}
      <section className="flex flex-1 flex-col">
        <div className="flex items-center justify-center gap-2 bg-brand-strong px-4 py-2 text-center text-sm font-semibold text-brand-foreground">
          <HeartPulseIcon className="size-4 shrink-0" aria-hidden="true" />
          Regístrate y gestiona todas tus citas del Centro de Salud Miguel Grau en línea
        </div>

        <div className="flex flex-1 items-center justify-center px-4 py-10 sm:px-8">
          <LoginForm />
        </div>

        <div className="flex flex-wrap items-center justify-center gap-x-5 gap-y-2 bg-muted px-4 py-5">
          <CircleHelpIcon className="size-9 text-primary" aria-hidden="true" />
          <p className="font-semibold text-primary">¿Primera vez en el portal?</p>
          <Link
            href="/ayuda"
            className="rounded-full border border-primary px-4 py-1.5 text-sm font-semibold text-primary outline-none hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            Ver centro de ayuda
          </Link>
        </div>

        <div className="grid grid-cols-1 border-t text-sm sm:grid-cols-2">
          <Link
            href="/ayuda"
            className="flex items-center justify-center gap-2 px-4 py-4 text-primary outline-none hover:underline focus-visible:underline sm:border-r"
          >
            <CircleHelpIcon className="size-4" aria-hidden="true" />
            Ayuda y tutoriales
          </Link>
          <a
            href="tel:106"
            className="flex items-center justify-center gap-2 px-4 py-4 text-destructive outline-none hover:underline focus-visible:underline"
          >
            <SirenIcon className="size-4" aria-hidden="true" />
            Emergencias SAMU 106
          </a>
        </div>
      </section>
    </main>
  )
}
