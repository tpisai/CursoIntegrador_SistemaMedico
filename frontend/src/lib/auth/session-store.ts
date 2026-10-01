import type { Sesion } from "@/lib/api/types"

// Sesión guardada en localStorage. Se lee con useSyncExternalStore (ver use-session.ts),
// por eso getSesion() devuelve la misma referencia mientras el valor no cambie.
const STORAGE_KEY = "saludgrau:sesion:v1"

const listeners = new Set<() => void>()
let cachedRaw: string | null = null
let cachedSesion: Sesion | null = null
// Respaldo cuando el navegador bloquea localStorage (modo privado estricto).
let memoria: string | null = null

function leer(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return memoria
  }
}

function escribir(raw: string | null) {
  memoria = raw
  try {
    if (raw) window.localStorage.setItem(STORAGE_KEY, raw)
    else window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Se queda solo en memoria.
  }
}

export function getSesion(): Sesion | null {
  if (typeof window === "undefined") return null
  const raw = leer()
  if (raw !== cachedRaw) {
    cachedRaw = raw
    try {
      cachedSesion = raw ? (JSON.parse(raw) as Sesion) : null
    } catch {
      cachedSesion = null
    }
  }
  return cachedSesion
}

export function setSesion(sesion: Sesion | null) {
  escribir(sesion ? JSON.stringify(sesion) : null)
  listeners.forEach((listener) => listener())
}

export function subscribeSesion(listener: () => void) {
  listeners.add(listener)
  const onStorage = (event: StorageEvent) => {
    if (event.key === STORAGE_KEY) listener()
  }
  window.addEventListener("storage", onStorage)
  return () => {
    listeners.delete(listener)
    window.removeEventListener("storage", onStorage)
  }
}
