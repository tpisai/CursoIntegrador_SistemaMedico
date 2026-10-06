import { request } from "./http"
import type {
  AtencionRegistrada,
  CambioFechaRequest,
  DerivacionRequest,
  DocumentoMedico,
  RegistrarAtencionRequest,
  Solicitud,
} from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-doctor.ts).
export const rutasDoctor = {
  citasDelDia: (fecha: string) => `/doctor/citas?fecha=${fecha}`,
  citasProximas: "/doctor/citas/proximas",
  horariosLibres: "/doctor/horarios-libres",
  solicitudes: "/doctor/solicitudes",
  tiposDocumento: "/doctor/tipos-documento",
}

export function solicitarCambioFecha(datos: CambioFechaRequest) {
  return request<Solicitud>("POST", "/doctor/solicitudes/cambio-fecha", datos)
}

export function solicitarDerivacion(datos: DerivacionRequest) {
  return request<Solicitud>("POST", "/doctor/solicitudes/derivacion", datos)
}

export function registrarAtencion(idCita: number, datos: RegistrarAtencionRequest) {
  return request<AtencionRegistrada>("POST", `/doctor/citas/${idCita}/atencion`, datos)
}

export function subirDocumento(idAtencion: number, tipoDocumento: string, archivo: File) {
  const formulario = new FormData()
  formulario.append("tipoDocumento", tipoDocumento)
  formulario.append("archivo", archivo)
  return request<DocumentoMedico>("POST", `/doctor/atenciones/${idAtencion}/documentos`, formulario)
}
