import type { Metadata } from "next"
import {
  CalendarClockIcon,
  CalendarDaysIcon,
  CircleHelpIcon,
  ClipboardListIcon,
  HospitalIcon,
  StethoscopeIcon,
} from "lucide-react"

import { Saludo } from "@/components/portal/saludo"
import { type Acceso, BannerAyuda, TarjetasAcceso, TituloPortal } from "@/components/portal/tarjetas-acceso"

export const metadata: Metadata = {
  title: "Inicio",
}

const ACCESOS: Acceso[] = [
  {
    href: "/doctor/agenda",
    titulo: "Atender pacientes",
    texto: "Abre la agenda de hoy e inicia la consulta de cada paciente.",
    boton: "Atender",
    Icono: StethoscopeIcon,
    color: "azul",
  },
  {
    href: "/doctor/agenda",
    titulo: "Mi agenda",
    texto: "Revisa tus citas programadas por día o por rango de fechas.",
    boton: "Ver agenda",
    Icono: CalendarDaysIcon,
    color: "verde",
  },
  {
    href: "/doctor/solicitudes#cambio-fecha",
    titulo: "Cambio de fecha",
    texto: "Solicita reprogramar una cita a administración.",
    boton: "Solicitar",
    Icono: CalendarClockIcon,
    color: "amarillo",
  },
  {
    href: "/doctor/solicitudes#derivacion",
    titulo: "Derivar paciente",
    texto: "Deriva a un paciente a otro hospital o especialidad.",
    boton: "Derivar",
    Icono: HospitalIcon,
    color: "celeste",
  },
  {
    href: "/doctor/solicitudes#mis-solicitudes",
    titulo: "Mis solicitudes",
    texto: "Sigue la respuesta de administración a tus solicitudes.",
    boton: "Ver",
    Icono: ClipboardListIcon,
    color: "violeta",
  },
  {
    href: "/doctor/ayuda",
    titulo: "Centro de ayuda",
    texto: "Tutoriales y guías paso a paso del portal médico.",
    boton: "Ver ayuda",
    Icono: CircleHelpIcon,
    color: "rosa",
  },
]

export default function InicioDoctorPage() {
  return (
    <>
      <TituloPortal
        titulo="Portal Médico · Centro de Salud Miguel Grau"
        subtitulo={
          <>
            <Saludo />, estas son tus herramientas de trabajo.
          </>
        }
      />
      <TarjetasAcceso accesos={ACCESOS} />
      <BannerAyuda href="/doctor/ayuda" />
    </>
  )
}
