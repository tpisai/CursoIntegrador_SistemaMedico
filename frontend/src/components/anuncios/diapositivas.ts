import {
  BabyIcon,
  FlaskConicalIcon,
  type LucideIcon,
  SaladIcon,
  SyringeIcon,
  UsersIcon,
} from "lucide-react"

export type DiapositivaInstitucional = {
  etiqueta: string
  titulo: string
  descripcion: string
  // Línea pequeña con horario o lugar.
  dato?: string
  enlace?: string
  textoBoton?: string
  Icono: LucideIcon
  // Degradado de fondo (clases de Tailwind para bg-linear-to-br).
  fondo: string
}

/**
 * Anuncios del centro de salud que el carrusel muestra mientras administración no publique
 * banners propios (Comunicación › Carrusel de eventos).
 */
export const DIAPOSITIVAS_INSTITUCIONALES: DiapositivaInstitucional[] = [
  {
    etiqueta: "Nueva especialidad",
    titulo: "Abrimos el servicio de Ecografía",
    descripcion:
      "Ecografía obstétrica, abdominal y pélvica sin salir del centro de salud. Separa tu cita desde el portal y recibe tu informe en el historial.",
    dato: "Lunes a sábado · Consultorios externos",
    enlace: "/paciente/agendar",
    textoBoton: "Agendar cita",
    Icono: BabyIcon,
    fondo: "from-rose-600 via-rose-500 to-orange-400",
  },
  {
    etiqueta: "Evento",
    titulo: "Feria de salud familiar",
    descripcion:
      "Tamizaje gratuito de glucosa, presión arterial, agudeza visual y salud bucal para toda la familia.",
    dato: "Patio principal del centro de salud · Trae tu DNI",
    enlace: "/paciente/comunicacion",
    textoBoton: "Ver campañas",
    Icono: UsersIcon,
    fondo: "from-cyan-800 via-cyan-600 to-teal-400",
  },
  {
    etiqueta: "Vacunación",
    titulo: "Protege a tu familia: vacúnate",
    descripcion:
      "Influenza, neumococo, VPH y el esquema regular para niños. Acércate con tu DNI y tu carné de vacunación.",
    dato: "Tópico de inmunizaciones · Lunes a sábado",
    Icono: SyringeIcon,
    fondo: "from-violet-700 via-indigo-600 to-sky-500",
  },
  {
    etiqueta: "Charla gratuita",
    titulo: "Alimentación saludable en las fiestas",
    descripcion:
      "Nuestras nutricionistas comparten consejos para disfrutar sin descuidar tu salud. Ingreso libre.",
    dato: "Auditorio del centro de salud · Cupos limitados",
    enlace: "/paciente/comunicacion",
    textoBoton: "Inscribirme",
    Icono: SaladIcon,
    fondo: "from-emerald-700 via-emerald-600 to-lime-500",
  },
  {
    etiqueta: "Horario extendido",
    titulo: "Laboratorio desde las 6:30 a. m.",
    descripcion:
      "Toma de muestras de lunes a sábado. Si tu examen lo requiere, recuerda venir en ayunas de 8 a 12 horas.",
    dato: "Resultados disponibles en tu historial médico",
    enlace: "/paciente/historial",
    textoBoton: "Ver mis resultados",
    Icono: FlaskConicalIcon,
    fondo: "from-[#0b2a5b] via-blue-800 to-cyan-600",
  },
]
