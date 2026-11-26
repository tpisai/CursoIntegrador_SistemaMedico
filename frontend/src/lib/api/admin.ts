import { request, requestArchivo } from "./http"
import type {
  AnuncioAdmin,
  CampaniaAdmin,
  CrearAnuncioDatos,
  CrearCampaniaRequest,
  CrearHorariosRequest,
  CrearHorariosResponse,
  ResponderSolicitudRequest,
  SolicitudAdmin,
  TicketRecepcion,
} from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-admin.ts).
export const rutasAdmin = {
  resumen: "/admin/resumen",
  solicitudes: "/admin/solicitudes",
  campanias: "/admin/campanias",
  anuncios: "/admin/anuncios",
  filtrosReporte: "/admin/reportes/filtros",
  reporteCitas: (desde: string, hasta: string, idEspecialidad?: number, idDoctor?: number, estado?: string) => {
    const params = new URLSearchParams({ desde, hasta })
    if (idEspecialidad) params.set("idEspecialidad", String(idEspecialidad))
    if (idDoctor) params.set("idDoctor", String(idDoctor))
    if (estado) params.set("estado", estado)
    return `/admin/reportes/citas?${params.toString()}`
  },
  reporteHorarios: (desde: string, hasta: string) => `/admin/reportes/horarios?desde=${desde}&hasta=${hasta}`,
  reportePrescripcion: (desde: string, hasta: string, idEspecialidad?: number, idDoctor?: number) =>
    `/admin/reportes/prescripcion?${parametrosPrescripcion(desde, hasta, idEspecialidad, idDoctor)}`,
}

function parametrosPrescripcion(desde: string, hasta: string, idEspecialidad?: number, idDoctor?: number) {
  const params = new URLSearchParams({ desde, hasta })
  if (idEspecialidad !== undefined) params.set("idEspecialidad", String(idEspecialidad))
  if (idDoctor !== undefined) params.set("idDoctor", String(idDoctor))
  return params.toString()
}

export function descargarReferenciaSolicitud(idSolicitud: number) {
  return requestArchivo(`/admin/solicitudes/${idSolicitud}/referencia`)
}

export function descargarReporteIntegralCitas(desde: string, hasta: string, secciones: string[]) {
  const params = new URLSearchParams({ desde, hasta })
  secciones.forEach((seccion) => params.append("secciones", seccion))
  return requestArchivo(`/admin/reportes/citas-integral/pdf?${params.toString()}`)
}

export function descargarReportePrescripcionPdf(
  desde: string,
  hasta: string,
  idEspecialidad?: number,
  idDoctor?: number
) {
  return requestArchivo(
    `/admin/reportes/prescripcion/pdf?${parametrosPrescripcion(desde, hasta, idEspecialidad, idDoctor)}`
  )
}

export function descargarReportePrescripcionCsv(
  desde: string,
  hasta: string,
  idEspecialidad?: number,
  idDoctor?: number
) {
  return requestArchivo(
    `/admin/reportes/prescripcion/csv?${parametrosPrescripcion(desde, hasta, idEspecialidad, idDoctor)}`
  )
}

export function responderSolicitud(idSolicitud: number, datos: ResponderSolicitudRequest) {
  return request<SolicitudAdmin>("PATCH", `/admin/solicitudes/${idSolicitud}`, datos)
}

export function crearHorarios(datos: CrearHorariosRequest) {
  return request<CrearHorariosResponse>("POST", "/admin/horarios", datos)
}

export function consultarTicket(numeroTicket: string) {
  return request<TicketRecepcion>("GET", `/admin/tickets/${encodeURIComponent(numeroTicket)}`)
}

export function validarTicket(numeroTicket: string) {
  return request<TicketRecepcion>("POST", `/admin/tickets/${encodeURIComponent(numeroTicket)}/validacion`)
}

export function crearCampania(datos: CrearCampaniaRequest) {
  return request<CampaniaAdmin>("POST", "/admin/campanias", datos)
}

/** Multipart: los datos como campos y el banner en "imagen". */
export function crearAnuncio({ imagen, ...datos }: CrearAnuncioDatos) {
  const formulario = new FormData()
  for (const [campo, valor] of Object.entries(datos)) {
    if (valor) formulario.append(campo, valor)
  }
  formulario.append("imagen", imagen)
  return request<AnuncioAdmin>("POST", "/admin/anuncios", formulario)
}

export function cambiarVisibilidadAnuncio(idAnuncio: number, activo: boolean) {
  return request<AnuncioAdmin>("PATCH", `/admin/anuncios/${idAnuncio}?activo=${activo}`)
}

export function eliminarAnuncio(idAnuncio: number) {
  return request<void>("DELETE", `/admin/anuncios/${idAnuncio}`)
}
