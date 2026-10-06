import { getSesion, setSesion } from "@/lib/auth/session-store"

// Next.js reenvía /api/* al backend de Spring Boot (ver next.config.ts).
const API_URL = "/api"

export type Metodo = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message)
    this.name = "ApiError"
  }
}

const SIN_CONEXION = "No pudimos conectar con el servidor. Revisa tu conexión o que el backend esté encendido."

const MENSAJES_POR_ESTADO: Record<number, string> = {
  400: "Revisa los datos ingresados.",
  401: "Tu sesión expiró. Vuelve a iniciar sesión.",
  403: "No tienes permiso para realizar esta acción.",
  404: "No encontramos lo que buscabas.",
  409: "La información cambió mientras la revisabas. Inténtalo de nuevo.",
  500: "Ocurrió un error en el servidor. Inténtalo más tarde.",
}

export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return MENSAJES_POR_ESTADO[500]
}

async function leerMensaje(res: Response): Promise<string> {
  try {
    // Spring Boot responde { message } (ver ApiExceptionHandler).
    const data = (await res.json()) as { message?: string; detail?: string }
    if (data.message || data.detail) return (data.message ?? data.detail)!
  } catch {
    // Cuerpo vacío o no JSON: suele ser el proxy de Next.js sin backend detrás.
    if (res.status >= 500) return SIN_CONEXION
  }
  return MENSAJES_POR_ESTADO[res.status] ?? MENSAJES_POR_ESTADO[500]
}

async function enviar(metodo: Metodo, path: string, body?: unknown): Promise<Response> {
  const token = getSesion()?.token

  let res: Response
  try {
    // Un FormData (archivos) lo envía el navegador con su propio Content-Type multipart.
    const esFormulario = body instanceof FormData
    res = await fetch(`${API_URL}${path}`, {
      method: metodo,
      headers: {
        ...(body !== undefined && !esFormulario && { "Content-Type": "application/json" }),
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      body: body === undefined ? undefined : esFormulario ? body : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, SIN_CONEXION)
  }

  if (!res.ok) {
    // Token vencido: se cierra la sesión (el login responde 401 sin token, eso no cuenta).
    if (res.status === 401 && token) setSesion(null)
    throw new ApiError(res.status, await leerMensaje(res))
  }
  return res
}

export async function request<T>(metodo: Metodo, path: string, body?: unknown): Promise<T> {
  const res = await enviar(metodo, path, body)
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export async function requestArchivo(path: string): Promise<Blob> {
  const res = await enviar("GET", path)
  return res.blob()
}

export const fetcher = <T>(path: string) => request<T>("GET", path)
