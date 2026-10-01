"use client"

import useSWR from "swr"

import { rutas } from "@/lib/api/citas"
import type {
  AtencionHistorial,
  Cita,
  Consultorio,
  DisponibilidadDia,
  Doctor,
  DocumentoMedico,
  Especialidad,
  Horario,
} from "@/lib/api/types"

// RF-05/RF-06: la disponibilidad se vuelve a pedir sola para reflejar reservas de otros pacientes.
const TIEMPO_REAL = { refreshInterval: 10_000, revalidateOnFocus: true }

export function useEspecialidades() {
  return useSWR<Especialidad[]>(rutas.especialidades)
}

export function useDoctores(idEspecialidad: number | undefined) {
  return useSWR<Doctor[]>(idEspecialidad ? rutas.doctores(idEspecialidad) : null)
}

export function useConsultorios(idDoctor: number | undefined) {
  return useSWR<Consultorio[]>(idDoctor ? rutas.consultorios(idDoctor) : null)
}

export function useDisponibilidad(idDoctor: number | undefined, idConsultorio: number | undefined, mes: string) {
  return useSWR<DisponibilidadDia[]>(
    idDoctor && idConsultorio ? rutas.disponibilidad(idDoctor, idConsultorio, mes) : null,
    { ...TIEMPO_REAL, keepPreviousData: true }
  )
}

export function useHorarios(idDoctor: number | undefined, idConsultorio: number | undefined, fecha: string | null) {
  return useSWR<Horario[]>(
    idDoctor && idConsultorio && fecha ? rutas.horarios(idDoctor, idConsultorio, fecha) : null,
    TIEMPO_REAL
  )
}

export function useMisCitas() {
  return useSWR<Cita[]>(rutas.misCitas)
}

export function useHistorial() {
  return useSWR<AtencionHistorial[]>(rutas.historial)
}

export function useDocumentos() {
  return useSWR<DocumentoMedico[]>(rutas.documentos)
}
