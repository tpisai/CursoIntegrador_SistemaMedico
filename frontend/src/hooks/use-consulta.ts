"use client"

import useSWR from "swr"

import { rutasConsulta } from "@/lib/api/consulta"
import type { CitaAsignada, ConsultaDoctor } from "@/lib/api/types"

// La agenda se refresca sola para ver las reservas nuevas y las llegadas que registra recepción.
const AGENDA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useAgenda(desde: string, hasta: string) {
  return useSWR<CitaAsignada[]>(rutasConsulta.agenda(desde, hasta), { ...AGENDA, keepPreviousData: true })
}

// Sin refresco automático: mientras el doctor escribe la ficha, la página no debe cambiar sola.
export function useConsulta(idCita: number | null) {
  return useSWR<ConsultaDoctor>(idCita ? rutasConsulta.consulta(idCita) : null, { revalidateOnFocus: false })
}
