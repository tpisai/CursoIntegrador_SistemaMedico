"use client"

import useSWR from "swr"

import { rutasSolicitudes } from "@/lib/api/solicitudes"
import type { CambioPaciente, CambioPendiente } from "@/lib/api/types"

// Un cambio de fecha aprobado bloquea el portal hasta que el paciente decide.
const AL_DIA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useCambiosPendientes() {
  return useSWR<CambioPendiente[]>(rutasSolicitudes.pendientes, AL_DIA)
}

export function useMisCambios() {
  return useSWR<CambioPaciente[]>(rutasSolicitudes.mias)
}
