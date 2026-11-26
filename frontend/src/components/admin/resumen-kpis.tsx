"use client"

import { useState } from "react"
import { CalendarDaysIcon, ClipboardListIcon, MegaphoneIcon, UserXIcon } from "lucide-react"

import { DatoHero } from "@/components/portal/hero-seccion"
import { useResumenAdmin } from "@/hooks/use-admin"
import { formatFechaLarga, toFechaISO } from "@/lib/format"

// Indicadores de la cabecera del panel de administración (pantalla 04 del prototipo).
export function ResumenKpis() {
  const { data } = useResumenAdmin()

  return (
    <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <DatoHero Icono={CalendarDaysIcon} valor={data?.citasHoy ?? "…"} texto="Citas programadas hoy" />
      <DatoHero Icono={ClipboardListIcon} valor={data?.solicitudesPendientes ?? "…"} texto="Solicitudes pendientes" />
      <DatoHero Icono={MegaphoneIcon} valor={data?.campaniasVigentes ?? "…"} texto="Campañas vigentes" />
      <DatoHero Icono={UserXIcon} valor={data ? `${data.ausentismoMes} %` : "…"} texto="Ausentismo del mes" />
    </ul>
  )
}

/** "Martes 25 de noviembre", calculado en el navegador. */
export function FechaHoy() {
  const [hoy] = useState(() => toFechaISO(new Date()))
  return <>{formatFechaLarga(hoy).toLowerCase()}</>
}
