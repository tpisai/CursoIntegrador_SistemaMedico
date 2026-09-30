"use client"

import useSWR from "swr"

import { rutasDoctor } from "@/lib/api/doctor"
import type { CitaAsignada, HorarioLibre, Solicitud } from "@/lib/api/types"

// La agenda se refresca sola para ver las reservas nuevas de los pacientes.
const AGENDA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useCitasDelDia(fecha: string) {
  return useSWR<CitaAsignada[]>(rutasDoctor.citasDelDia(fecha), AGENDA)
}

export function useCitasProximas() {
  return useSWR<CitaAsignada[]>(rutasDoctor.citasProximas, AGENDA)
}

export function useHorariosLibres() {
  return useSWR<HorarioLibre[]>(rutasDoctor.horariosLibres)
}

export function useSolicitudes() {
  return useSWR<Solicitud[]>(rutasDoctor.solicitudes)
}
