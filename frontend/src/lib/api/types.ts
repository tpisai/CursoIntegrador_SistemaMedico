  lugar: string | null
  estado: EstadoCampania
  cupos: number
  cuposDisponibles: number
  // Inscripción del paciente; ambos null si aún no se inscribe.
  idInscripcion: number | null
  // Código del comprobante, p. ej. "C-000012".
  codigoInscripcion: string | null
}

export interface Inscripcion {
  idInscripcion: number
  // Código del comprobante, p. ej. "C-000012".
  codigo: string
  idCampania: number
  campania: string
  fechaInscripcion: string
}

// --- Notificaciones ---

export interface Notificacion {
  idNotificacion: number
  tipo: "CITA" | "SOLICITUD" | "CAMPANIA" | "SISTEMA" | "RECORDATORIO"
  titulo: string
  mensaje: string
  leida: boolean
  // "YYYY-MM-DDTHH:mm:ss"
  fechaEnvio: string
}

// --- Consulta del doctor: paciente, historia clínica y exámenes ---

/** CONSULTA: ficha de consulta. APOYO_DIAGNOSTICO: laboratorio, ecografía y rayos X reportan exámenes. */
export type TipoEspecialidad = "CONSULTA" | "APOYO_DIAGNOSTICO"

export interface PacienteConsulta {
  idPaciente: number
  nombreCompleto: string
  dni: string
  fechaNacimiento: string | null
  edad: number | null
  sexo: string | null
  grupoSanguineo: string | null
  telefono: string | null
  correo: string
  direccion: string | null
  contactoEmergencia: string | null
  telefonoEmergencia: string | null
  numeroHistoria: string | null
  alergias: string | null
  antecedentes: string | null
  medicacionHabitual: string | null
}

export interface MedicamentoRecetado {
  nombreMedicamento: string
  dosis: string | null
  presentacion: string | null
  viaAdministracion: string | null
  unidadesPorToma: string | null
  frecuencia: string | null
  duracion: string | null
  cantidad: string | null
}

export interface ResultadoExamen {
  tipoExamen: string
  resultado: string
  valorReferencia: string | null
  interpretacion: string | null
}

export interface AtencionPrevia {
  idAtencion: number
  idCita: number
  fecha: string
  especialidad: string
  tipoEspecialidad: TipoEspecialidad
  doctor: string
  // La registró el doctor que consulta: solo él descarga su receta y sus documentos clínicos.
  propia: boolean
  // La cita tiene una solicitud de derivación: se puede emitir la referencia.
  tieneDerivacion: boolean
  // La atención emitió una orden de exámenes: el doctor puede descargarla.
  tieneOrdenExamen: boolean
  anamnesis: string | null
  examenFisico: string | null
  diagnostico: string | null
  tratamiento: string | null
  observaciones: string | null
  medicamentos: MedicamentoRecetado[]
  resultados: ResultadoExamen[]
  documentos: DocumentoMedico[]
}

export interface ConsultaDoctor {
  cita: CitaAsignada
  especialidad: string
  modo: TipoEspecialidad
  // La cita está activa y es de hoy o de un día anterior.
  puedeAtender: boolean
  // Atención ya registrada para esta cita; null si aún no se atiende.
  idAtencion: number | null
  paciente: PacienteConsulta
  historial: AtencionPrevia[]
}

export interface ResultadoExamenRequest {
  tipoExamen: string
  resultado: string
  valorReferencia?: string
  interpretacion?: string
}

export interface AntecedentesRequest {
  alergias: string
  antecedentes: string
  medicacionHabitual: string
}
