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

/** Documentos clínicos en PDF de una atención propia. La referencia exige una solicitud de derivación. */
export type DocumentoClinico = "ficha-consulta" | "informe-medico" | "referencia"

export function descargarDocumentoClinico(idAtencion: number, documento: DocumentoClinico) {
  return requestArchivo(`/doctor/atenciones/${idAtencion}/${documento}`)
}

/** Todas las fichas de consulta del paciente de una cita del doctor, en un solo PDF. */
export function descargarFichasDelPaciente(idCita: number) {
  return requestArchivo(`/doctor/citas/${idCita}/fichas-consulta`)
}
