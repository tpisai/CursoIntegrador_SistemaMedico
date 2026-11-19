import {
  BellRingIcon,
  CalendarClockIcon,
  CalendarPlusIcon,
  CalendarXIcon,
  FileDownIcon,
  FileHeartIcon,
  HospitalIcon,
  KeyRoundIcon,
  MegaphoneIcon,
  StethoscopeIcon,
  TicketIcon,
  type LucideIcon,
} from "lucide-react"

export type PerfilAyuda = "paciente" | "doctor"

export type Tutorial = { titulo: string; pregunta: string; Icono: LucideIcon; pasos: string[] }
export type Guia = { titulo: string; pasos: string[] }
export type Pregunta = { pregunta: string; respuesta: string }

export type ContenidoAyuda = {
  titulo: string
  descripcion: string
  tutoriales: Tutorial[]
  guias: Guia[]
  preguntas: Pregunta[]
}

const PACIENTE: ContenidoAyuda = {
  titulo: "Centro de ayuda para pacientes",
  descripcion: "Aprende a reservar, consultar y cancelar tus citas, y a revisar tu historial médico en línea.",
  tutoriales: [
    {
      titulo: "Agendamiento de citas médicas",
      pregunta: "¿Cómo agendar una cita médica?",
      Icono: CalendarPlusIcon,
      pasos: [
        "En el inicio del portal pulsa «Solicitar» en la tarjeta «Solicita tu cita» (o el botón «Agendar cita» de la barra superior).",
        "En «Buscar disponibilidad» elige la especialidad y, si lo deseas, el doctor o el consultorio.",
        "En el calendario selecciona un día con turnos disponibles.",
        "Elige el horario que prefieras y escribe brevemente el motivo de tu consulta.",
        "Confirma la reserva. Se generará tu ticket de atención y recibirás una notificación.",
      ],
    },
    {
      titulo: "Cancelar citas médicas",
      pregunta: "¿Cómo cancelar una cita médica?",
      Icono: CalendarXIcon,
      pasos: [
        "Entra a «Consulta tus citas» desde el inicio o a «Mis citas» en la barra superior.",
        "Busca la cita en «Mis citas activas».",
        "Pulsa «Cancelar» y confirma en la ventana que aparece.",
        "El turno queda libre para otro paciente. Si necesitas otra fecha, vuelve a agendar una cita.",
      ],
    },
    {
      titulo: "Ver el ticket de tu cita",
      pregunta: "¿Dónde veo mi ticket de atención?",
      Icono: TicketIcon,
      pasos: [
        "En el inicio verás tu próxima cita con el botón «Ver ticket».",
        "También puedes abrirlo desde «Mis citas», en cada cita activa.",
        "Presenta el número de ticket en Admisión el día de tu cita, 15 minutos antes del horario.",
      ],
    },
    {
      titulo: "Revisar tu historial médico",
      pregunta: "¿Cómo reviso mi historial médico?",
      Icono: FileHeartIcon,
      pasos: [
        "Pulsa «Ver historial» en la tarjeta «Historial médico» del inicio.",
        "Verás tus atenciones anteriores ordenadas de la más reciente a la más antigua.",
        "Abre una atención para ver el diagnóstico, la receta y los documentos o resultados adjuntos.",
        "Descarga en PDF los documentos que necesites.",
      ],
    },
    {
      titulo: "Inscribirte en campañas de salud",
      pregunta: "¿Cómo me inscribo en una campaña?",
      Icono: MegaphoneIcon,
      pasos: [
        "Entra a «Campañas» en la barra superior.",
        "Revisa las campañas vigentes, su fecha y los cupos disponibles.",
        "Pulsa «Inscribirme» y confirma tus datos.",
        "Recibirás la confirmación de tu inscripción en las notificaciones.",
      ],
    },
    {
      titulo: "Recuperar tu PIN",
      pregunta: "¿Olvidaste tu PIN de acceso?",
      Icono: KeyRoundIcon,
      pasos: [
        "En la pantalla de inicio de sesión pulsa «Olvidé mi PIN».",
        "Escribe el correo electrónico con el que te registraste.",
        "Ingresa el código de verificación que te llegó por correo.",
        "Crea un nuevo PIN de 6 dígitos y vuelve a iniciar sesión.",
      ],
    },
  ],
  guias: [
    {
      titulo: "Crear tu cuenta de paciente",
      pasos: [
        "En la pantalla de inicio de sesión pulsa «Regístrate».",
        "Elige el tipo de cuenta «Paciente».",
        "Completa tus nombres, apellidos, DNI y correo electrónico.",
        "Crea un PIN numérico de 6 dígitos y repítelo para confirmarlo.",
        "Acepta los Términos y Condiciones y la política de protección de datos (Ley N.° 29733) y pulsa «Crear cuenta».",
      ],
    },
    {
      titulo: "Qué llevar el día de tu cita",
      pasos: [
        "Tu DNI físico.",
        "El número de ticket de tu cita (puedes mostrarlo desde el celular).",
        "Resultados de exámenes o recetas anteriores relacionados con tu consulta.",
        "Llega 15 minutos antes para pasar por Admisión y Triaje.",
      ],
    },
    {
      titulo: "Activar y leer tus notificaciones",
      pasos: [
        "Pulsa el icono de la campana en la barra superior.",
        "Los avisos sin leer aparecen resaltados con un punto de color.",
        "Pulsa un aviso para marcarlo como leído o usa «Marcar todas como leídas».",
      ],
    },
  ],
  preguntas: [
    {
      pregunta: "¿Puedo reservar una cita para una emergencia?",
      respuesta:
        "No. Ante una emergencia acude directamente al área de Emergencia o llama al SAMU (106). El portal es solo para citas programadas.",
    },
    {
      pregunta: "¿Cuántos días antes puedo reservar?",
      respuesta:
        "Puedes reservar en cualquier horario que el calendario muestre como disponible. Los horarios se publican según la programación de cada especialidad.",
    },
    {
      pregunta: "¿Tiene algún costo usar el portal?",
      respuesta: "No. Crear tu cuenta, reservar y consultar tus citas en línea es gratuito.",
    },
    {
      pregunta: "¿Mis datos están protegidos?",
      respuesta:
        "Sí. Tus datos personales y de salud se tratan conforme a la Ley N.° 29733 de Protección de Datos Personales y solo los ve el personal autorizado.",
    },
  ],
}

const DOCTOR: ContenidoAyuda = {
  titulo: "Centro de ayuda para el personal médico",
  descripcion: "Guías para atender consultas, registrar recetas y exámenes, y gestionar tus solicitudes.",
  tutoriales: [
    {
      titulo: "Atender una consulta",
      pregunta: "¿Cómo atiendo a un paciente?",
      Icono: StethoscopeIcon,
      pasos: [
        "En el inicio pulsa «Atender» o entra a «Agenda» en la barra superior.",
        "Busca al paciente en la lista de citas de hoy y pulsa «Atender».",
        "Revisa la historia clínica y los antecedentes del paciente.",
        "Completa la ficha de consulta: motivo y síntomas, examen físico, diagnóstico, tratamiento y conclusión.",
        "Guarda la atención. La cita pasará al estado «Atendida».",
      ],
    },
    {
      titulo: "Registrar receta y exámenes",
      pregunta: "¿Cómo emito una receta médica?",
      Icono: FileHeartIcon,
      pasos: [
        "Dentro de la consulta ve a la sección de receta.",
        "Agrega cada medicamento con su presentación, dosis, frecuencia, vía de administración y duración.",
        "Si el paciente necesita exámenes, indícalos en la orden de exámenes.",
        "Guarda la atención para que la receta y la orden queden en el historial del paciente.",
      ],
    },
    {
      titulo: "Solicitar cambio de fecha",
      pregunta: "¿Cómo reprogramo una cita?",
      Icono: CalendarClockIcon,
      pasos: [
        "Entra a «Solicitudes» en la barra superior.",
        "En «Solicitar cambio de fecha» elige la cita y escribe el motivo.",
        "Envía la solicitud. Administración la revisará antes de avisar al paciente.",
        "Sigue la respuesta en «Mis solicitudes».",
      ],
    },
    {
      titulo: "Derivar a un paciente",
      pregunta: "¿Cómo derivo a otro hospital?",
      Icono: HospitalIcon,
      pasos: [
        "Entra a «Solicitudes» y ve a «Derivar paciente a otro hospital».",
        "Elige al paciente, el establecimiento de destino y la especialidad.",
        "Describe el motivo clínico de la derivación.",
        "Envía la solicitud y revisa su estado en «Mis solicitudes».",
      ],
    },
    {
      titulo: "Descargar documentos PDF",
      pregunta: "¿Cómo descargo constancias y certificados?",
      Icono: FileDownIcon,
      pasos: [
        "En «Agenda» busca la cita atendida.",
        "Usa los botones «Constancia» o «Certificado» para descargar el documento en PDF.",
        "Desde la consulta también puedes generar la ficha, el informe médico y la referencia.",
      ],
    },
    {
      titulo: "Revisar tus notificaciones",
      pregunta: "¿Dónde veo las respuestas de administración?",
      Icono: BellRingIcon,
      pasos: [
        "Pulsa la campana de la barra superior para ver tus avisos.",
        "Cada respuesta a una solicitud llega como notificación.",
        "El detalle completo de cada solicitud está en «Solicitudes» › «Mis solicitudes».",
      ],
    },
  ],
  guias: [
    {
      titulo: "Crear tu cuenta de doctor",
      pasos: [
        "En la pantalla de inicio de sesión pulsa «Regístrate».",
        "Elige el tipo de cuenta «Doctor».",
        "Completa tus datos, tu especialidad y tu número de CMP (5 o 6 dígitos).",
        "Crea tu PIN de 6 dígitos y acepta los términos para terminar.",
      ],
    },
    {
      titulo: "Revisar la agenda por rango de fechas",
      pasos: [
        "Entra a «Agenda».",
        "Usa el selector de fechas para ver un día distinto o un rango de días.",
        "Las citas de hoy muestran «Atender»; las atendidas, «Ver atención», y las futuras, «Ver historia».",
      ],
    },
    {
      titulo: "Adjuntar resultados y documentos",
      pasos: [
        "Dentro de la consulta pulsa la opción para adjuntar un documento.",
        "Selecciona un archivo PDF, JPG o PNG de hasta 5 MB.",
        "El documento quedará disponible en el historial del paciente.",
      ],
    },
  ],
  preguntas: [
    {
      pregunta: "¿Puedo editar una atención ya guardada?",
      respuesta:
        "Una vez registrada, la atención forma parte de la historia clínica. Si necesitas corregirla, comunícate con administración.",
    },
    {
      pregunta: "¿Por qué mi solicitud sigue pendiente?",
      respuesta:
        "Los cambios de fecha y las derivaciones requieren aprobación de administración. Recibirás una notificación cuando respondan.",
    },
    {
      pregunta: "¿Qué hago si el paciente no asiste?",
      respuesta: "Abre la cita desde tu agenda y regístrala como inasistencia para que quede en el historial.",
    },
  ],
}

export const CONTENIDO_AYUDA: Record<PerfilAyuda, ContenidoAyuda> = { paciente: PACIENTE, doctor: DOCTOR }
