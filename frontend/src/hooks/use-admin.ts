"use client"

import useSWR from "swr"

import { rutasAdmin } from "@/lib/api/admin"
import type { AnuncioAdmin, CampaniaAdmin, ReporteCitas, ReporteOcupacionHorarios, ReportePrescripcion, ResumenAdmin, SolicitudAdmin } from "@/lib/api/types"

// Las solicitudes de los doctores llegan en cualquier momento.
const AL_DIA = { refreshInterval: 30_000, revalidateOnFocus: true }

export function useResumenAdmin() {
  return useSWR<ResumenAdmin>(rutasAdmin.resumen, AL_DIA)
}

export function useSolicitudesAdmin() {
  return useSWR<SolicitudAdmin[]>(rutasAdmin.solicitudes, AL_DIA)
}

export function useReporteCitas(desde: string | null, hasta: string | null) {
  return useSWR<ReporteCitas>(desde && hasta ? rutasAdmin.reporteCitas(desde, hasta) : null)
}

export function useReporteOcupacionHorarios(desde: string | null, hasta: string | null) {
  return useSWR<ReporteOcupacionHorarios>(desde && hasta ? rutasAdmin.reporteHorarios(desde, hasta) : null)
}

export function useReportePrescripcion(
  desde: string | null,
  hasta: string | null,
  idEspecialidad?: number,
  idDoctor?: number
) {
  const ruta = desde && hasta
    ? rutasAdmin.reportePrescripcion(desde, hasta, idEspecialidad, idDoctor)
    : null
  return useSWR<ReportePrescripcion>(ruta)
}

export function useCampaniasAdmin() {
  return useSWR<CampaniaAdmin[]>(rutasAdmin.campanias)
}

export function useAnunciosAdmin() {
  return useSWR<AnuncioAdmin[]>(rutasAdmin.anuncios)
}
