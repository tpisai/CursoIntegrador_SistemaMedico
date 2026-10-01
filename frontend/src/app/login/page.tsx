import type { Metadata } from "next"
import { CheckIcon } from "lucide-react"

import { Logo } from "@/components/brand/logo"
import { LoginForm } from "@/components/auth/login-form"
import { RedirigirSiHaySesion } from "@/components/auth/redirigir-si-hay-sesion"

export const metadata: Metadata = {
  title: "Iniciar sesión",
}

const BENEFICIOS = [
  "Citas en línea disponibles 24/7",
  "Historial médico y resultados digitales",
  "Alertas y recordatorios de tus citas",
]

// Pantalla 01 · Login del prototipo de Figma.
export default function LoginPage() {
  return (
    <main className="flex min-h-svh flex-1 flex-col lg:grid lg:grid-cols-[minmax(0,620px)_1fr]">
      <RedirigirSiHaySesion />

      <section className="flex flex-col justify-center gap-3 bg-linear-to-b from-brand to-brand-strong px-6 py-8 text-brand-foreground lg:gap-5 lg:px-[72px]">
        <Logo className="text-2xl lg:gap-3.5 lg:text-4xl" markClassName="size-10 lg:size-16" />
        <p className="font-medium opacity-90 lg:text-[17px]">Centro de Salud Miguel Grau · Chaclacayo</p>
        <p className="hidden max-w-[440px] leading-[26px] opacity-85 lg:block">
          Reserva tus citas médicas en línea, consulta la disponibilidad de especialidades y accede a tu
          historial médico digital sin colas ni llamadas.
        </p>
        <ul className="hidden flex-col gap-5 lg:flex">
          {BENEFICIOS.map((beneficio) => (
            <li key={beneficio} className="flex items-center gap-3 text-[15px] font-medium">
              <span className="flex size-[26px] items-center justify-center rounded-full bg-white/20">
                <CheckIcon className="size-3.5" strokeWidth={3} aria-hidden="true" />
              </span>
              {beneficio}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex flex-1 items-center justify-center px-4 py-10 sm:px-8">
        <LoginForm />
      </section>
    </main>
  )
}
