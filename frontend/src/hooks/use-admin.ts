"use client"

import useSWR from "swr"

import { rutasAdmin } from "@/lib/api/admin"
import type { CampaniaAdmin, ReporteCitas, ResumenAdmin, SolicitudAdmin } from "@/lib/api/types"

// Las solicitudes de los doctores llegan en cualquier momento.
const AL_DIA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useResumenAdmin() {
  return useSWR<ResumenAdmin>(rutasAdmin.resumen, AL_DIA)
}

export function useSolicitudesPendientes() {
  return useSWR<SolicitudAdmin[]>(rutasAdmin.solicitudes, AL_DIA)
}

export function useReporteCitas(desde: string | null, hasta: string | null) {
  return useSWR<ReporteCitas>(desde && hasta ? rutasAdmin.reporteCitas(desde, hasta) : null)
}

export function useCampaniasAdmin() {
  return useSWR<CampaniaAdmin[]>(rutasAdmin.campanias)
}
