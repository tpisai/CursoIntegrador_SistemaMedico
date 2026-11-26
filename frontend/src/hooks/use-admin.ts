"use client"

import useSWR from "swr"

import { rutasAdmin } from "@/lib/api/admin"
import type { CampaniaAdmin, FiltrosReporte, ReporteCitas, ReporteOcupacionHorarios, ResumenAdmin, SolicitudAdmin } from "@/lib/api/types"

// Las solicitudes de los doctores llegan en cualquier momento.
const AL_DIA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useResumenAdmin() {
  return useSWR<ResumenAdmin>(rutasAdmin.resumen, AL_DIA)
}

export function useSolicitudesAdmin() {
  return useSWR<SolicitudAdmin[]>(rutasAdmin.solicitudes, AL_DIA)
}

export function useFiltrosReporte() {
  return useSWR<FiltrosReporte>(rutasAdmin.filtrosReporte)
}

export function useReporteCitas(
  desde: string | null,
  hasta: string | null,
  idEspecialidad?: number,
  idDoctor?: number,
  estado?: string,
) {
  return useSWR<ReporteCitas>(
    desde && hasta ? rutasAdmin.reporteCitas(desde, hasta, idEspecialidad, idDoctor, estado) : null,
  )
}

export function useReporteOcupacionHorarios(desde: string | null, hasta: string | null) {
  return useSWR<ReporteOcupacionHorarios>(desde && hasta ? rutasAdmin.reporteHorarios(desde, hasta) : null)
}

export function useCampaniasAdmin() {
  return useSWR<CampaniaAdmin[]>(rutasAdmin.campanias)
}
