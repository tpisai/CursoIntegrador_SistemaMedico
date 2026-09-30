"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { toast } from "sonner"

import { CalendarioDisponibilidad } from "@/components/citas/calendario-disponibilidad"
import { FiltrosDisponibilidad } from "@/components/citas/filtros-disponibilidad"
import { SelectorHorarios } from "@/components/citas/selector-horarios"
import { type EstadoTicket, TicketCitaDialog } from "@/components/citas/ticket-cita-dialog"
import {
  useConsultorios,
  useDisponibilidad,
  useDoctores,
  useEspecialidades,
  useHorarios,
} from "@/hooks/use-citas"
import { reservarCita, rutas } from "@/lib/api/citas"
import { ApiError, mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import type { Horario } from "@/lib/api/types"
import { toMesISO } from "@/lib/format"

const MESES_A_FUTURO = 2

function inicioDeMes(fecha: Date, desplazamiento = 0) {
  return new Date(fecha.getFullYear(), fecha.getMonth() + desplazamiento, 1)
}

/** La opción elegida si sigue en la lista; si no, la primera (como en el prototipo). */
function opcionVigente<T>(elegida: number | null, lista: T[] | undefined, id: (item: T) => number) {
  if (!lista?.length) return undefined
  return elegida !== null && lista.some((item) => id(item) === elegida) ? elegida : id(lista[0])
}

// Pantalla 02 · Portal del Paciente: RF-05 (disponibilidad), RF-06 (reserva) y RF-08 (ticket).
export function ReservaCita() {
  const { mutate } = useSWRConfig()
  const [hoy] = useState(() => new Date())
  const [especialidadElegida, setEspecialidadElegida] = useState<number | null>(null)
  const [doctorElegido, setDoctorElegido] = useState<number | null>(null)
  const [consultorioElegido, setConsultorioElegido] = useState<number | null>(null)
  // null = el calendario elige el mes solo (ver mesAutomatico).
  const [mesElegido, setMesElegido] = useState<Date | null>(null)
  const [fecha, setFecha] = useState<string | null>(null)
  const [idHorario, setIdHorario] = useState<number | null>(null)
  const [ticket, setTicket] = useState<EstadoTicket | null>(null)
  const [reservando, startTransition] = useTransition()

  const especialidades = useEspecialidades()
  const idEspecialidad = opcionVigente(especialidadElegida, especialidades.data, (e) => e.idEspecialidad)
  const doctores = useDoctores(idEspecialidad)
  const idDoctor = opcionVigente(doctorElegido, doctores.data, (d) => d.idDoctor)
  const consultorios = useConsultorios(idDoctor)
  const idConsultorio = opcionVigente(consultorioElegido, consultorios.data, (c) => c.idConsultorio)

  // Si en lo que queda del mes actual ya no hay cupos (p. ej. el último día), se abre el siguiente.
  // Cuando el mes mostrado es el actual, ambas consultas comparten la misma clave de SWR.
  const mesActual = inicioDeMes(hoy)
  const cuposMesActual = useDisponibilidad(idDoctor, idConsultorio, toMesISO(mesActual))
  const mesAutomatico = cuposMesActual.data?.length === 0 ? inicioDeMes(hoy, 1) : mesActual
  const mes = mesElegido ?? mesAutomatico
  const disponibilidad = useDisponibilidad(idDoctor, idConsultorio, toMesISO(mes))
  const horarios = useHorarios(idDoctor, idConsultorio, fecha)

  function limpiarFecha() {
    setMesElegido(null)
    setFecha(null)
    setIdHorario(null)
  }

  function elegirEspecialidad(id: number) {
    setEspecialidadElegida(id)
    setDoctorElegido(null)
    setConsultorioElegido(null)
    limpiarFecha()
  }

  function elegirDoctor(id: number) {
    setDoctorElegido(id)
    setConsultorioElegido(null)
    limpiarFecha()
  }

  function elegirConsultorio(id: number) {
    setConsultorioElegido(id)
    limpiarFecha()
  }

  function elegirFecha(nueva: string) {
    setFecha(nueva)
    setIdHorario(null)
  }

  function confirmar(horario: Horario) {
    startTransition(async () => {
      try {
        const cita = await reservarCita({ idHorario: horario.idHorario })
        setIdHorario(null)
        setTicket({ cita, recienReservada: true, abierto: true })
      } catch (error) {
        toast.error(mensajeDeError(error))
        if (!(error instanceof ApiError && error.status === 409)) return
      }
      // Tras reservar (o si otro paciente ganó el cupo) se refresca todo lo que depende de los cupos.
      await Promise.all([
        horarios.mutate(),
        disponibilidad.mutate(),
        mutate(rutas.misCitas),
        mutate(RUTA_NOTIFICACIONES),
      ])
    })
  }

  return (
    <>
      <section
        aria-label="Reservar cita"
        className="grid gap-6 md:grid-cols-[minmax(0,300px)_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)_300px]"
      >
        <FiltrosDisponibilidad
          especialidades={especialidades.data}
          doctores={doctores.data}
          consultorios={consultorios.data}
          idEspecialidad={idEspecialidad}
          idDoctor={idDoctor}
          idConsultorio={idConsultorio}
          onEspecialidad={elegirEspecialidad}
          onDoctor={elegirDoctor}
          onConsultorio={elegirConsultorio}
        />
        <CalendarioDisponibilidad
          mes={mes}
          mesMinimo={mesActual}
          mesMaximo={inicioDeMes(hoy, MESES_A_FUTURO)}
          onMes={setMesElegido}
          fecha={fecha}
          onFecha={elegirFecha}
          disponibilidad={disponibilidad.data}
          cargando={
            especialidades.isLoading ||
            doctores.isLoading ||
            consultorios.isLoading ||
            cuposMesActual.isLoading ||
            disponibilidad.isLoading
          }
        />
        <div className="md:col-span-2 xl:col-span-1 [&>*]:h-full">
          <SelectorHorarios
            fecha={fecha}
            horarios={horarios.data}
            cargando={horarios.isLoading}
            idHorario={idHorario}
            onHorario={setIdHorario}
            onConfirmar={confirmar}
            reservando={reservando}
          />
        </div>
      </section>

      <TicketCitaDialog
        ticket={ticket}
        onCerrar={() => setTicket((t) => t && { ...t, abierto: false })}
      />
    </>
  )
}
