"use client"

import { useEffect, useState } from "react"
import {
  BellRingIcon,
  CalendarCheckIcon,
  ChevronRightIcon,
  FileHeartIcon,
  MegaphoneIcon,
  type LucideIcon,
} from "lucide-react"

import { cn } from "@/lib/utils"

type Diapositiva = {
  titulo: string
  texto: string
  etiqueta: string
  Icono: LucideIcon
}

const DIAPOSITIVAS: Diapositiva[] = [
  {
    titulo: "Reserva tus citas en línea",
    texto: "Elige especialidad, doctor y horario sin colas ni llamadas",
    etiqueta: "Agendar cita",
    Icono: CalendarCheckIcon,
  },
  {
    titulo: "Accede a tu historial médico",
    texto: "Revisa tus atenciones, recetas y resultados cuando quieras",
    etiqueta: "Historial médico",
    Icono: FileHeartIcon,
  },
  {
    titulo: "Recibe recordatorios de tus citas",
    texto: "Te avisamos antes de cada atención para que no la pierdas",
    etiqueta: "Notificaciones",
    Icono: BellRingIcon,
  },
  {
    titulo: "Participa en campañas de salud",
    texto: "Inscríbete gratis en las campañas del Centro de Salud Miguel Grau",
    etiqueta: "Campañas de salud",
    Icono: MegaphoneIcon,
  },
]

const INTERVALO_MS = 6000

/** Carrusel del panel izquierdo del login: maqueta de laptop con la función destacada. */
export function LoginCarrusel() {
  const [activa, setActiva] = useState(0)
  const [pausado, setPausado] = useState(false)

  useEffect(() => {
    if (pausado) return
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return
    const id = window.setInterval(() => setActiva((i) => (i + 1) % DIAPOSITIVAS.length), INTERVALO_MS)
    return () => window.clearInterval(id)
  }, [pausado])

  const { titulo, texto, etiqueta, Icono } = DIAPOSITIVAS[activa]

  return (
    <div
      className="flex flex-col items-center gap-8 text-center"
      onMouseEnter={() => setPausado(true)}
      onMouseLeave={() => setPausado(false)}
      onFocus={() => setPausado(true)}
      onBlur={() => setPausado(false)}
    >
      <MaquetaLaptop key={activa} etiqueta={etiqueta} Icono={Icono} indice={activa} />

      <div key={`t-${activa}`} className="flex min-h-[132px] flex-col gap-3" aria-live="polite">
        <h2 className="text-balance text-3xl leading-tight font-bold xl:text-[34px]">{titulo}</h2>
        <p className="text-balance text-lg opacity-90">{texto}</p>
      </div>

      <div className="flex items-center gap-2" role="group" aria-label="Diapositivas">
        {DIAPOSITIVAS.map((d, i) => (
          <button
            key={d.titulo}
            type="button"
            aria-label={`Ver diapositiva ${i + 1}: ${d.titulo}`}
            aria-current={i === activa ? "true" : undefined}
            onClick={() => setActiva(i)}
            className={cn(
              "h-2.5 rounded-full bg-white/50 outline-none transition-all hover:bg-white/80 focus-visible:ring-3 focus-visible:ring-white/60",
              i === activa ? "w-12 bg-white" : "w-2.5"
            )}
          />
        ))}
      </div>
    </div>
  )
}

function MaquetaLaptop({ etiqueta, Icono, indice }: { etiqueta: string; Icono: LucideIcon; indice: number }) {
  return (
    <div aria-hidden="true" className="relative w-full max-w-[460px]">
      <span className="absolute -top-4 right-2 text-4xl font-thin opacity-80">+</span>
      <span className="absolute top-1/2 -left-4 text-4xl font-thin opacity-80">+</span>

      {/* Pantalla */}
      <div className="mx-auto w-[86%] rounded-t-xl border-[6px] border-b-0 border-slate-200 bg-white p-2 shadow-2xl">
        <div className="mb-2 flex items-center gap-1">
          <span className="size-1.5 rounded-full bg-red-400" />
          <span className="size-1.5 rounded-full bg-amber-400" />
          <span className="size-1.5 rounded-full bg-emerald-400" />
          <span className="ml-2 h-1.5 flex-1 rounded-full bg-slate-100" />
        </div>
        <div className="flex gap-2">
          <div className="flex w-1/5 flex-col gap-1.5 border-r border-slate-100 pr-2">
            {Array.from({ length: 6 }, (_, i) => (
              <span key={i} className={cn("h-1.5 rounded-full", i === indice + 1 ? "bg-cyan-500" : "bg-slate-200")} />
            ))}
          </div>
          <div className="flex flex-1 flex-col gap-1.5 text-left">
            <span className="h-2 w-1/3 rounded-full bg-slate-300" />
            <div className="grid grid-cols-4 gap-1">
              {Array.from({ length: 4 }, (_, i) => (
                <span key={i} className="h-3 rounded bg-slate-100" />
              ))}
            </div>
            {Array.from({ length: 4 }, (_, i) => (
              <div key={i} className="flex items-center gap-1.5 rounded border border-slate-100 p-1">
                <span className="size-2.5 rounded-full bg-cyan-100" />
                <span className="h-1.5 flex-1 rounded-full bg-slate-200" />
                <span className="h-1.5 w-6 rounded-full bg-lime-300" />
              </div>
            ))}
          </div>
        </div>
      </div>
      {/* Base */}
      <div className="mx-auto h-3 w-full rounded-b-2xl bg-linear-to-b from-slate-200 to-slate-300 shadow-lg" />

      {/* Tarjeta flotante */}
      <div className="absolute right-0 bottom-8 flex items-center gap-3 rounded-xl bg-white px-4 py-3 text-slate-800 shadow-xl sm:-right-6">
        <Icono className="size-6 text-cyan-700" />
        <span className="text-left text-sm leading-tight font-semibold">{etiqueta}</span>
        <ChevronRightIcon className="size-5 text-cyan-700" />
      </div>
    </div>
  )
}
