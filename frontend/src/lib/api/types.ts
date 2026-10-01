// DTOs que intercambia el frontend con la API (records de citas_salud/.../api/dto).
// Los estados replican los CHECK de sql/citas_salud.sql; las fechas viajan como
// "YYYY-MM-DD" y las horas como "HH:mm".

export type Rol = "PACIENTE" | "DOCTOR" | "ADMISION" | "ADMINISTRADOR"

export interface Usuario {
  idUsuario: number
  dni: string
  nombres: string
  apellidos: string
  correo: string
  rol: Rol
  // Nombre de la especialidad cuando el usuario es doctor.
  especialidad: string | null
}

export interface Sesion {
  token: string
  usuario: Usuario
}

export interface LoginRequest {
  dni: string
  pin: string
}

export type TipoCuenta = "PACIENTE" | "DOCTOR"

export interface RegistroRequest {
  tipoCuenta: TipoCuenta
  nombres: string
  apellidos: string
  dni: string
  correo: string
  pin: string
  terminosAceptados: boolean
  // Solo para cuentas de doctor.
  idEspecialidad?: number
  cmp?: string
}

export interface RecuperarPinRequest {
  correo: string
}

export interface Especialidad {
  idEspecialidad: number
  nombre: string
}

export interface Doctor {
  idDoctor: number
  idEspecialidad: number
  // Nombre ya formateado por el backend, p. ej. "Dr. Rodrigo Mendoza".
  nombreCompleto: string
}

export interface Consultorio {
  idConsultorio: number
  nombre: string
  zona: string | null
  piso: string | null
  numero: string | null
}

export interface DisponibilidadDia {
  fecha: string
  cuposDisponibles: number
}

export type EstadoHorario = "DISPONIBLE" | "RESERVADO" | "CANCELADO" | "FINALIZADO"

export interface Horario {
  idHorario: number
  fecha: string
  horaInicio: string
  horaFin: string
  estado: EstadoHorario
}

export type EstadoCita = "RESERVADA" | "CONFIRMADA" | "ATENDIDA" | "CANCELADA" | "NO_ASISTIO"

export interface Cita {
  idCita: number
  numeroTicket: string
  estado: EstadoCita
  fecha: string
  horaInicio: string
  horaFin: string
  especialidad: string
  doctor: string
  // Ubicación ya formateada, p. ej. "Módulo A · Consultorio 3".
  consultorio: string
}

export interface ReservarCitaRequest {
  idHorario: number
  motivo?: string
}

export interface AtencionHistorial {
  idCita: number
  fecha: string
  especialidad: string
  doctor: string
  estado: Extract<EstadoCita, "ATENDIDA" | "NO_ASISTIO">
}

export interface DocumentoMedico {
  idDocumento: number
  tipoDocumento: string
  nombreArchivo: string
  fechaCarga: string
}

// --- Panel del doctor ---

export interface CitaAsignada {
  idCita: number
  numeroTicket: string
  estado: EstadoCita
  fecha: string
  horaInicio: string
  horaFin: string
  // "Luis Ramírez H."
  paciente: string
  dniPaciente: string
  motivo: string | null
  consultorio: string
}

export interface HorarioLibre {
  idHorario: number
  fecha: string
  horaInicio: string
  consultorio: string
}

export interface CambioFechaRequest {
  idCita: number
  idHorarioNuevo: number
  motivo: string
}

export interface DerivacionRequest {
  idCita: number
  establecimiento: string
  especialidad: string
  motivo: string
}

export type TipoSolicitud = "CAMBIO_HORARIO" | "DERIVACION"
export type EstadoSolicitud = "PENDIENTE" | "EN_REVISION" | "APROBADA" | "RECHAZADA"

export interface Solicitud {
  idSolicitud: number
  tipo: TipoSolicitud
  estado: EstadoSolicitud
  // "YYYY-MM-DDTHH:mm:ss"
  fechaSolicitud: string
  paciente: string
  fechaCita: string
  horaCita: string
  // Nueva fecha propuesta o establecimiento de destino, ya en texto.
  detalle: string | null
  motivo: string
  respuestaAdmin: string | null
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
