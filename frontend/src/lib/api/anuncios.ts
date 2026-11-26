// Carrusel de eventos del portal del paciente (público: no exige sesión).
export const RUTA_ANUNCIOS = "/anuncios"

/** Tamaño recomendado de los banners: rectangular 15:8. Debe coincidir con AnuncioService.java. */
export const BANNER = {
  ancho: 1200,
  alto: 640,
  anchoMinimo: 960,
  altoMinimo: 512,
  tolerancia: 0.06,
  maxBytes: 5 * 1024 * 1024,
} as const

/** URL del <img>: el backend sirve la imagen en /api/anuncios/{id}/imagen. */
export function srcImagenAnuncio(imagenUrl: string) {
  return `/api${imagenUrl}`
}

/** null si la imagen sirve para el carrusel; si no, el motivo para mostrar al administrador. */
export function problemaMedidas(ancho: number, alto: number): string | null {
  const recomendado = `Usa ${BANNER.ancho} × ${BANNER.alto} px (proporción 15:8). La tuya mide ${ancho} × ${alto} px.`
  if (ancho < BANNER.anchoMinimo || alto < BANNER.altoMinimo) return `La imagen es muy pequeña. ${recomendado}`
  const esperada = BANNER.ancho / BANNER.alto
  if (Math.abs(ancho / alto - esperada) / esperada > BANNER.tolerancia) {
    return `La imagen no tiene la proporción del carrusel. ${recomendado}`
  }
  return null
}
