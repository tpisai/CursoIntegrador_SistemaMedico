import Link from "next/link"
import {
  AmbulanceIcon,
  ArrowRightIcon,
  BabyIcon,
  BrainIcon,
  ClockIcon,
  type LucideIcon,
  MonitorSmartphoneIcon,
  PhoneIcon,
  SaladIcon,
  ScanLineIcon,
  SmileIcon,
  StethoscopeIcon,
  SyringeIcon,
} from "lucide-react"

import { CarruselEventos } from "@/components/anuncios/carrusel-eventos"

const HORARIOS = [
  { area: "Consulta externa", horario: "Lun a sáb · 7:00 a. m. – 7:00 p. m." },
  { area: "Laboratorio", horario: "Lun a sáb · 6:30 a. m. – 11:00 a. m." },
  { area: "Farmacia", horario: "Lun a dom · 7:00 a. m. – 10:00 p. m." },
]

/** Carrusel de novedades junto a los horarios y teléfonos útiles del centro. */
export function NovedadesInicio() {
  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-8">
      <CarruselEventos />

      <aside className="flex flex-col gap-4">
        <section
          aria-labelledby="horarios-centro"
          className="flex flex-col gap-4 rounded-2xl bg-card p-5 shadow-[0_6px_24px_rgb(14_60_90/0.07)] ring-1 ring-foreground/[0.07]"
        >
          <h2 id="horarios-centro" className="flex items-center gap-2 text-base font-bold">
            <ClockIcon className="size-5 text-primary" aria-hidden="true" />
            Horarios de atención
          </h2>
          <dl className="flex flex-col gap-3">
            {HORARIOS.map(({ area, horario }) => (
              <div key={area} className="flex flex-col gap-0.5 border-l-[3px] border-primary/60 pl-3">
                <dt className="text-sm font-semibold">{area}</dt>
                <dd className="text-[13px] text-muted-foreground">{horario}</dd>
              </div>
            ))}
          </dl>
        </section>

        <section
          aria-labelledby="emergencias-centro"
          className="flex flex-col gap-3 rounded-2xl bg-linear-to-br from-rose-600 to-red-500 p-5 text-white shadow-[0_10px_30px_rgb(200_40_50/0.25)]"
        >
          <h2 id="emergencias-centro" className="flex items-center gap-2 text-base font-bold">
            <AmbulanceIcon className="size-5" aria-hidden="true" />
            Emergencias las 24 horas
          </h2>
          <p className="text-[13px] text-white/90">Ante una emergencia no reserves cita: acude directo o llama.</p>
          <div className="flex flex-wrap gap-2">
            <a
              href="tel:106"
              className="flex h-9 items-center gap-1.5 rounded-full bg-white px-4 text-sm font-bold text-rose-700 outline-none hover:bg-white/90 focus-visible:ring-3 focus-visible:ring-white/60"
            >
              <PhoneIcon className="size-4" aria-hidden="true" />
              SAMU 106
            </a>
            <a
              href="tel:113"
              className="flex h-9 items-center gap-1.5 rounded-full bg-white/15 px-4 text-sm font-semibold ring-1 ring-white/40 outline-none hover:bg-white/25 focus-visible:ring-3 focus-visible:ring-white/60"
            >
              Infosalud 113
            </a>
          </div>
        </section>
      </aside>
    </div>
  )
}

const ESPECIALIDADES: { nombre: string; Icono: LucideIcon; nuevo?: boolean }[] = [
  { nombre: "Medicina General", Icono: StethoscopeIcon },
  { nombre: "Pediatría", Icono: BabyIcon },
  { nombre: "Odontología", Icono: SmileIcon },
  { nombre: "Nutrición", Icono: SaladIcon },
  { nombre: "Psicología", Icono: BrainIcon },
  { nombre: "Inmunizaciones", Icono: SyringeIcon },
  { nombre: "Ecografía", Icono: ScanLineIcon, nuevo: true },
  { nombre: "Telesalud", Icono: MonitorSmartphoneIcon, nuevo: true },
]

/** Franja con las especialidades del centro; cada una lleva a agendar cita. */
export function EspecialidadesDestacadas() {
  return (
    <section aria-labelledby="especialidades-centro" className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id="especialidades-centro" className="text-2xl font-bold tracking-tight">
            Nuestras especialidades
          </h2>
          <p className="text-muted-foreground">Atención integral para toda la familia en un solo lugar.</p>
        </div>
        <Link
          href="/paciente/agendar"
          className="flex items-center gap-1.5 text-sm font-semibold text-primary outline-none hover:underline focus-visible:underline"
        >
          Agendar una cita
          <ArrowRightIcon className="size-4" aria-hidden="true" />
        </Link>
      </div>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
        {ESPECIALIDADES.map(({ nombre, Icono, nuevo }) => (
          <li key={nombre}>
            <Link
              href="/paciente/agendar"
              className="relative flex h-full flex-col items-center gap-2.5 rounded-2xl bg-card px-3 py-5 text-center shadow-[0_6px_24px_rgb(14_60_90/0.06)] ring-1 ring-foreground/[0.07] outline-none transition-[translate,box-shadow] hover:-translate-y-1 hover:shadow-[0_12px_30px_rgb(14_60_90/0.12)] focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              {nuevo ? (
                <span className="absolute top-2 right-2 rounded-full bg-lime px-1.5 py-px text-[10px] font-bold text-lime-foreground">
                  NUEVO
                </span>
              ) : null}
              <span className="flex size-12 items-center justify-center rounded-2xl bg-secondary text-primary">
                <Icono className="size-6" aria-hidden="true" />
              </span>
              <span className="text-[13px] leading-tight font-semibold">{nombre}</span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
