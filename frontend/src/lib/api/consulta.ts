import { request, requestArchivo } from "./http"
import type { AntecedentesRequest, PacienteConsulta, ResultadoExamen, ResultadoExamenRequest } from "./types"

// Rutas GET usadas como claves de SWR (ver src/hooks/use-consulta.ts).
export const rutasConsulta = {
  agenda: (desde: string, hasta: string) => `/doctor/agenda?desde=${desde}&hasta=${hasta}`,
  consulta: (idCita: number) => `/doctor/citas/${idCita}/consulta`,
}

/** Todo lo que cambia al atender: agenda, consulta abierta y citas para las solicitudes. */
export const esClaveDelDoctor = (clave: unknown) => typeof clave === "string" && clave.startsWith("/doctor/")

export function guardarAntecedentes(idCita: number, datos: AntecedentesRequest) {
  return request<PacienteConsulta>("PATCH", `/doctor/citas/${idCita}/antecedentes`, datos)
}

export function registrarResultados(idAtencion: number, resultados: ResultadoExamenRequest[]) {
  return request<ResultadoExamen[]>("POST", `/doctor/atenciones/${idAtencion}/resultados`, { resultados })
}

/** Documento de un paciente que el doctor atiende. */
export function descargarDocumentoDePaciente(idDocumento: number) {
  return requestArchivo(`/doctor/documentos/${idDocumento}/descarga`)
}
