import { request, requestArchivo } from "./http"
import type { Cita, ReservarCitaRequest } from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-citas.ts).
export const rutas = {
  especialidades: "/especialidades",
  doctores: (idEspecialidad: number) => `/doctores?especialidad=${idEspecialidad}`,
  consultorios: (idDoctor: number) => `/doctores/${idDoctor}/consultorios`,
  disponibilidad: (idDoctor: number, idConsultorio: number, mes: string) =>
    `/horarios/disponibilidad?doctor=${idDoctor}&consultorio=${idConsultorio}&mes=${mes}`,
  horarios: (idDoctor: number, idConsultorio: number, fecha: string) =>
    `/horarios?doctor=${idDoctor}&consultorio=${idConsultorio}&fecha=${fecha}`,
  misCitas: "/citas/mias",
  historial: "/pacientes/me/historial",
  documentos: "/pacientes/me/documentos",
}

export function reservarCita(datos: ReservarCitaRequest) {
  return request<Cita>("POST", "/citas", datos)
}

export function cancelarCita(idCita: number) {
  return request<Cita>("PATCH", `/citas/${idCita}/cancelar`)
}

export function descargarTicket(idCita: number) {
  return requestArchivo(`/citas/${idCita}/ticket`)
}

export function descargarDocumento(idDocumento: number) {
  return requestArchivo(`/documentos/${idDocumento}/descarga`)
}
