// Formatos de fecha del prototipo ("Mar 22 sep · 09:30"). Se usan tablas fijas en vez de
// Intl porque cada navegador abrevia distinto ("sep" vs "sept").
const DIAS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"]
const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
const MESES_LARGOS = [
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre",
]

/** "2026-09-22" -> Date en hora local (sin desfase de zona horaria). */
export function parseFecha(fecha: string): Date {
  const [anio, mes, dia] = fecha.split("-").map(Number)
  return new Date(anio, mes - 1, dia)
}

/** Date -> "2026-09-22" */
export function toFechaISO(date: Date): string {
  const mes = String(date.getMonth() + 1).padStart(2, "0")
  const dia = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${mes}-${dia}`
}

/** Date -> "2026-09" */
export function toMesISO(date: Date): string {
  return toFechaISO(date).slice(0, 7)
}

/** "Mar 22 sep" */
export function formatDiaCorto(fecha: string): string {
  const d = parseFecha(fecha)
  return `${DIAS[d.getDay()]} ${d.getDate()} ${MESES[d.getMonth()]}`
}

/** "Mar 22 sep · 09:30" */
export function formatDiaHora(fecha: string, hora: string): string {
  return `${formatDiaCorto(fecha)} · ${hora}`
}

/** "Miércoles 30 de septiembre" */
export function formatFechaLarga(fecha: string): string {
  const d = parseFecha(fecha)
  const dias = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"]
  return `${dias[d.getDay()]} ${d.getDate()} de ${MESES_LARGOS[d.getMonth()].toLowerCase()}`
}

/** "12 ago 2026" */
export function formatFecha(fecha: string): string {
  const d = parseFecha(fecha)
  return `${d.getDate()} ${MESES[d.getMonth()]} ${d.getFullYear()}`
}

/** "Septiembre 2026" */
export function formatMesAnio(date: Date): string {
  return `${MESES_LARGOS[date.getMonth()]} ${date.getFullYear()}`
}

/** Encabezados del calendario: "Lu", "Ma", "Mi"... */
export function formatDiaSemana(date: Date): string {
  return ["Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sa"][date.getDay()]
}

/** "hace 5 min", "ayer", "12 ago 2026". Recibe "YYYY-MM-DDTHH:mm:ss" en hora local. */
export function tiempoRelativo(fechaHora: string, ahora = new Date()): string {
  const fecha = new Date(fechaHora)
  const minutos = Math.floor((ahora.getTime() - fecha.getTime()) / 60_000)
  if (minutos < 1) return "hace un momento"
  if (minutos < 60) return `hace ${minutos} min`
  const horas = Math.floor(minutos / 60)
  if (horas < 24) return horas === 1 ? "hace 1 hora" : `hace ${horas} horas`
  const dias = Math.floor(horas / 24)
  if (dias === 1) return "ayer"
  if (dias < 7) return `hace ${dias} días`
  return formatFecha(toFechaISO(fecha))
}

export function iniciales(nombres: string, apellidos: string): string {
  return `${nombres.trim()[0] ?? ""}${apellidos.trim()[0] ?? ""}`.toUpperCase()
}

export function primerNombre(nombres: string): string {
  return nombres.trim().split(/\s+/)[0] ?? ""
}
