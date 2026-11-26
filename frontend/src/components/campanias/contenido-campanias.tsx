"use client"

import { useState } from "react"
import Link from "next/link"
import {
  ActivityIcon,
  ArrowRightIcon,
  BellRingIcon,
  CalendarHeartIcon,
  DropletIcon,
  GiftIcon,
  HandHeartIcon,
  type LucideIcon,
  PhoneIcon,
  ShieldCheckIcon,
  SunIcon,
  SyringeIcon,
  TicketIcon,
  UsersIcon,
} from "lucide-react"

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

// ----- Beneficios -----

const BENEFICIOS: { Icono: LucideIcon; titulo: string; texto: string; tono: string }[] = [
  { Icono: GiftIcon, titulo: "100 % gratuitas", texto: "Sin costo para la comunidad", tono: "bg-emerald-100 text-emerald-700" },
  { Icono: TicketIcon, titulo: "Comprobante digital", texto: "Descárgalo al inscribirte", tono: "bg-sky-100 text-sky-700" },
  { Icono: BellRingIcon, titulo: "Te recordamos", texto: "Aviso un día antes", tono: "bg-amber-100 text-amber-700" },
  { Icono: ShieldCheckIcon, titulo: "Personal certificado", texto: "Médicos y enfermeras del centro", tono: "bg-violet-100 text-violet-700" },
]

export function BeneficiosCampanias() {
  return (
    <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-5">
      {BENEFICIOS.map(({ Icono, titulo, texto, tono }) => (
        <li
          key={titulo}
          className="flex items-center gap-3 rounded-2xl bg-card p-4 shadow-[0_6px_24px_rgb(14_60_90/0.06)] ring-1 ring-foreground/[0.07]"
        >
          <span className={cn("flex size-11 shrink-0 items-center justify-center rounded-xl dark:bg-muted dark:text-foreground", tono)}>
            <Icono className="size-5" aria-hidden="true" />
          </span>
          <span className="flex min-w-0 flex-col">
            <span className="text-sm font-bold">{titulo}</span>
            <span className="text-xs text-muted-foreground">{texto}</span>
          </span>
        </li>
      ))}
    </ul>
  )
}

// ----- Banner del programa del adulto mayor -----

export function BannerAdultoMayor() {
  return (
    <section
      aria-labelledby="banner-adulto-mayor"
      className="relative overflow-hidden rounded-3xl bg-linear-to-r from-navy via-[#123a7a] to-cyan-700 text-white shadow-[0_14px_40px_rgb(11_42_91/0.25)]"
    >
      <span aria-hidden="true" className="absolute -top-20 right-1/4 size-64 rounded-full bg-white/[0.06]" />
      <span aria-hidden="true" className="absolute -right-10 -bottom-24 size-72 rounded-full bg-cyan-400/20" />
      <div className="relative grid items-center gap-6 p-6 sm:p-8 md:grid-cols-[minmax(0,1fr)_auto] lg:p-10">
        <div className="flex flex-col gap-3">
          <span className="w-fit rounded-full bg-lime px-3 py-0.5 text-xs font-bold tracking-wide text-lime-foreground uppercase">
            Programa permanente
          </span>
          <h2 id="banner-adulto-mayor" className="text-2xl leading-tight font-bold text-balance lg:text-[32px]">
            Club del adulto mayor: muévete, comparte y cuida tu salud
          </h2>
          <p className="max-w-[60ch] text-white/85">
            Talleres de actividad física, memoria y alimentación para personas de 60 años a más. Control de presión y
            glucosa en cada sesión. Inscríbete en admisión con tu DNI.
          </p>
          <ul className="flex flex-wrap gap-2 pt-1 text-xs font-semibold">
            <li className="rounded-full bg-white/15 px-3 py-1 ring-1 ring-white/25">Martes y jueves · 9:00 a. m.</li>
            <li className="rounded-full bg-white/15 px-3 py-1 ring-1 ring-white/25">Auditorio del centro</li>
            <li className="rounded-full bg-white/15 px-3 py-1 ring-1 ring-white/25">Sin costo</li>
          </ul>
        </div>
        <div aria-hidden="true" className="hidden grid-cols-2 gap-3 md:grid">
          {[ActivityIcon, HandHeartIcon, UsersIcon, SunIcon].map((Icono, i) => (
            <span
              key={i}
              className={cn(
                "flex size-24 items-center justify-center rounded-3xl ring-1 ring-white/20 lg:size-28",
                i % 3 === 0 ? "bg-white/15" : "bg-white/[0.08]"
              )}
            >
              <Icono className="size-10 lg:size-12" strokeWidth={1.6} />
            </span>
          ))}
        </div>
      </div>
    </section>
  )
}

// ----- Consejos de prevención -----

const CONSEJOS: { Icono: LucideIcon; titulo: string; texto: string; tono: string }[] = [
  {
    Icono: SyringeIcon,
    titulo: "Mantén tus vacunas al día",
    texto: "Revisa tu carné y el de tus hijos. La vacuna contra la influenza se aplica cada año.",
    tono: "from-violet-500 to-indigo-500",
  },
  {
    Icono: ActivityIcon,
    titulo: "Controla tu presión",
    texto: "Desde los 18 años mídela al menos una vez al año; cada 6 meses si tienes antecedentes.",
    tono: "from-rose-500 to-orange-400",
  },
  {
    Icono: DropletIcon,
    titulo: "Hidrátate en verano",
    texto: "Toma de 6 a 8 vasos de agua al día y evita el sol directo entre las 10 a. m. y las 4 p. m.",
    tono: "from-sky-500 to-cyan-400",
  },
  {
    Icono: HandHeartIcon,
    titulo: "Lava bien tus manos",
    texto: "Con agua y jabón durante 20 segundos, antes de comer y después de ir al baño.",
    tono: "from-emerald-500 to-teal-400",
  },
]

export function ConsejosPrevencion() {
  return (
    <section aria-labelledby="consejos-prevencion" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 id="consejos-prevencion" className="text-2xl font-bold tracking-tight">
          Consejos de prevención
        </h2>
        <p className="text-muted-foreground">Pequeños hábitos que protegen tu salud y la de tu familia.</p>
      </div>
      <ul className="grid gap-4 sm:grid-cols-2">
        {CONSEJOS.map(({ Icono, titulo, texto, tono }) => (
          <li
            key={titulo}
            className="group flex gap-4 rounded-2xl bg-card p-5 shadow-[0_6px_24px_rgb(14_60_90/0.06)] ring-1 ring-foreground/[0.07]"
          >
            <span
              className={cn(
                "flex size-12 shrink-0 items-center justify-center rounded-2xl bg-linear-to-br text-white shadow-md transition-transform group-hover:scale-105",
                tono
              )}
            >
              <Icono className="size-6" aria-hidden="true" />
            </span>
            <div className="flex flex-col gap-1">
              <h3 className="font-semibold">{titulo}</h3>
              <p className="text-sm leading-relaxed text-muted-foreground">{texto}</p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

// ----- Calendario de salud (fechas mundiales) -----

const FECHAS_SALUD: { mes: number; dia: number; titulo: string; texto: string }[] = [
  { mes: 2, dia: 4, titulo: "Día Mundial contra el Cáncer", texto: "La detección temprana salva vidas." },
  { mes: 3, dia: 24, titulo: "Día Mundial de la Tuberculosis", texto: "Tos por más de 15 días: hazte la prueba." },
  { mes: 4, dia: 7, titulo: "Día Mundial de la Salud", texto: "Salud para todas las personas." },
  { mes: 5, dia: 12, titulo: "Día Internacional de la Enfermería", texto: "Gracias a quienes nos cuidan." },
  { mes: 5, dia: 31, titulo: "Día Mundial Sin Tabaco", texto: "Dejar de fumar mejora tu salud desde el primer día." },
  { mes: 6, dia: 14, titulo: "Día Mundial del Donante de Sangre", texto: "Una donación puede salvar hasta tres vidas." },
  { mes: 7, dia: 28, titulo: "Día Mundial contra la Hepatitis", texto: "Vacúnate contra la hepatitis B." },
  { mes: 8, dia: 1, titulo: "Semana Mundial de la Lactancia Materna", texto: "Lactancia exclusiva hasta los 6 meses." },
  { mes: 9, dia: 29, titulo: "Día Mundial del Corazón", texto: "Controla tu presión y tu colesterol." },
  { mes: 10, dia: 10, titulo: "Día Mundial de la Salud Mental", texto: "Pedir ayuda también es cuidarse." },
  { mes: 10, dia: 19, titulo: "Día de la Lucha contra el Cáncer de Mama", texto: "Autoexamen mensual y mamografía desde los 40." },
  { mes: 11, dia: 14, titulo: "Día Mundial de la Diabetes", texto: "Mide tu glucosa al menos una vez al año." },
  { mes: 12, dia: 1, titulo: "Día Mundial de la Lucha contra el Sida", texto: "La prueba de VIH es gratuita y confidencial." },
  { mes: 12, dia: 3, titulo: "Día de las Personas con Discapacidad", texto: "Por una atención inclusiva y accesible." },
]
const MESES_CORTOS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]

/** Las próximas fechas a partir de hoy (pasado diciembre continúa con las del año siguiente). */
function proximasFechas(hoy: Date, cantidad: number) {
  const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())
  return FECHAS_SALUD.map((f) => {
    let fecha = new Date(hoy.getFullYear(), f.mes - 1, f.dia)
    if (fecha < inicio) fecha = new Date(hoy.getFullYear() + 1, f.mes - 1, f.dia)
    return { ...f, fecha, dias: Math.round((fecha.getTime() - inicio.getTime()) / 86_400_000) }
  })
    .sort((a, b) => a.fecha.getTime() - b.fecha.getTime())
    .slice(0, cantidad)
}

export function CalendarioSalud() {
  const [hoy] = useState(() => new Date())
  const fechas = proximasFechas(hoy, 5)

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 font-bold">
          <CalendarHeartIcon className="size-5 text-primary" aria-hidden="true" />
          Calendario de salud
        </CardTitle>
        <CardDescription>Fechas mundiales que promovemos en el centro.</CardDescription>
      </CardHeader>
      <CardContent>
        <ol className="flex flex-col gap-3">
          {fechas.map((f) => (
            <li key={f.titulo} className="flex items-center gap-3">
              <span className="flex w-14 shrink-0 flex-col items-center rounded-xl bg-secondary py-1.5 leading-none text-secondary-foreground">
                <span className="text-lg font-bold tabular-nums">{f.dia}</span>
                <span className="text-[11px] font-semibold uppercase">{MESES_CORTOS[f.mes - 1]}</span>
              </span>
              <span className="flex min-w-0 flex-col">
                <span className="text-[13px] leading-snug font-semibold">{f.titulo}</span>
                <span className="text-xs text-muted-foreground">
                  {f.dias === 0 ? "¡Es hoy! · " : f.dias === 1 ? "Mañana · " : `En ${f.dias} días · `}
                  {f.texto}
                </span>
              </span>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}

// ----- Preguntas frecuentes -----

const PREGUNTAS = [
  {
    p: "¿Las campañas tienen algún costo?",
    r: "No. Todas las campañas del Centro de Salud Miguel Grau son gratuitas para la comunidad. Solo necesitas inscribirte para reservar tu cupo.",
  },
  {
    p: "¿Qué debo llevar el día de la campaña?",
    r: "Tu DNI y el comprobante de inscripción (en el celular o impreso). Si la campaña es de vacunación, lleva también tu carné de vacunas.",
  },
  {
    p: "¿Puedo inscribir a un familiar?",
    r: "Cada paciente se inscribe desde su propia cuenta. Si tu familiar no tiene cuenta, puede crearla desde la pantalla de inicio de sesión con su DNI.",
  },
  {
    p: "¿Qué pasa si ya no puedo asistir?",
    r: "Avísanos en admisión para liberar tu cupo y que otra persona pueda aprovecharlo. Puedes inscribirte en la siguiente campaña sin problema.",
  },
  {
    p: "¿Cómo me entero de nuevas campañas?",
    r: "Cuando se publica una campaña te llega una notificación en la campana del portal. Además, un día antes te recordamos las campañas en las que te inscribiste.",
  },
]

export function PreguntasCampanias() {
  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl font-bold">Preguntas frecuentes</CardTitle>
          <CardDescription>Lo que más nos consultan sobre las campañas.</CardDescription>
        </CardHeader>
        <CardContent>
          <Accordion type="single" collapsible className="w-full">
            {PREGUNTAS.map(({ p, r }, i) => (
              <AccordionItem key={p} value={`p-${i}`}>
                <AccordionTrigger className="text-[15px] font-semibold">{p}</AccordionTrigger>
                <AccordionContent className="leading-relaxed text-muted-foreground">{r}</AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </CardContent>
      </Card>

      <section
        aria-labelledby="contacto-campanias"
        className="flex flex-col justify-between gap-5 rounded-2xl bg-linear-to-br from-lime to-[color-mix(in_oklch,var(--lime),white_35%)] p-6 text-lime-foreground shadow-[0_10px_30px_rgb(120_150_20/0.2)]"
      >
        <div className="flex flex-col gap-2">
          <span className="flex size-12 items-center justify-center rounded-2xl bg-white/60">
            <PhoneIcon className="size-6" aria-hidden="true" />
          </span>
          <h2 id="contacto-campanias" className="text-xl font-bold">
            ¿Tu comunidad necesita una campaña?
          </h2>
          <p className="text-sm">
            Juntas vecinales, colegios y comedores populares pueden solicitar campañas de salud. Acércate a admisión
            o llama a Infosalud (113) para orientación.
          </p>
        </div>
        <Link
          href="/paciente/ayuda"
          className="flex h-11 w-fit items-center gap-2 rounded-full bg-lime-foreground px-5 text-sm font-semibold text-white outline-none hover:opacity-90 focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          Ir al centro de ayuda
          <ArrowRightIcon className="size-4" aria-hidden="true" />
        </Link>
      </section>
    </div>
  )
}
