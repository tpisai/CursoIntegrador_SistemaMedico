import type { Metadata } from "next"

import { HistorialPaciente } from "@/components/historial/historial-paciente"

export const metadata: Metadata = {
  title: "Historial médico",
}

export default function HistorialPage() {
  return <HistorialPaciente />
}
