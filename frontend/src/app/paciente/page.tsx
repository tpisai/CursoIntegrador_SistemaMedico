import type { Metadata } from "next"
import {
  CalendarPlusIcon,
  CircleHelpIcon,
  FileHeartIcon,
  FlaskConicalIcon,
  MegaphoneIcon,
  SearchIcon,
} from "lucide-react"

import { ProximaCita } from "@/components/citas/proxima-cita"
import { Saludo } from "@/components/portal/saludo"
import { type Acceso, BannerAyuda, TarjetasAcceso, TituloPortal } from "@/components/portal/tarjetas-acceso"

export const metadata: Metadata = {
  title: "Inicio",
}

const ACCESOS: Acceso[] = [
  {
    href: "/paciente/agendar",
    titulo: "Solicita tu cita",
    texto: "Agenda una nueva cita médica de forma rápida y segura.",
    boton: "Solicitar",
    Icono: CalendarPlusIcon,
    color: "azul",
  },
  {
    href: "/paciente/citas",
    titulo: "Consulta tus citas",
    texto: "Revisa el estado, el ticket y los detalles de tus citas programadas.",
    boton: "Consultar",
    Icono: SearchIcon,
    color: "verde",
  },
  {
    href: "/paciente/historial",
    titulo: "Historial médico",
    texto: "Revisa tus atenciones anteriores, diagnósticos y recetas.",
    boton: "Ver historial",
    Icono: FileHeartIcon,
    color: "amarillo",
  },
  {
    href: "/paciente/historial",
    titulo: "Resultados y documentos",
    texto: "Descarga tus resultados de laboratorio y documentos médicos.",
    boton: "Ver",
    Icono: FlaskConicalIcon,
    color: "celeste",
  },
  {
    href: "/paciente/comunicacion",
    titulo: "Campañas de salud",
    texto: "Inscríbete en las campañas gratuitas para la comunidad.",
    boton: "Ver campañas",
    Icono: MegaphoneIcon,
    color: "violeta",
  },
  {
    href: "/paciente/ayuda",
    titulo: "Centro de ayuda",
    texto: "Tutoriales y guías paso a paso para usar el portal.",
    boton: "Ver ayuda",
    Icono: CircleHelpIcon,
    color: "rosa",
  },
]

export default function InicioPacientePage() {
  return (
    <>
      <TituloPortal
        titulo="Portal del Paciente · Centro de Salud Miguel Grau"
        subtitulo={
          <>
            <Saludo />, ¿qué necesitas hacer hoy?
          </>
        }
      />
      <ProximaCita />
      <TarjetasAcceso accesos={ACCESOS} />
      <BannerAyuda href="/paciente/ayuda" />
    </>
  )
}
