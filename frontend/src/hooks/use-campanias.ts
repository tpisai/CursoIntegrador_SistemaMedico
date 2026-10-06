"use client"

import useSWR from "swr"

import { RUTA_CAMPANIAS } from "@/lib/api/campanias"
import type { Campania } from "@/lib/api/types"

/** Campañas que ve el paciente, con su inscripción si la tiene. */
export function useCampanias() {
  return useSWR<Campania[]>(RUTA_CAMPANIAS, { revalidateOnFocus: true })
}
