import type { Metadata } from "next"
import { notFound } from "next/navigation"

import { ConsultaDoctor } from "@/components/consulta/consulta-doctor"

export const metadata: Metadata = {
  title: "Consulta",
}

// Página a la que lleva el botón Atender de la agenda.
export default async function ConsultaPage({ params }: PageProps<"/doctor/consulta/[idCita]">) {
  const { idCita } = await params
  if (!/^\d+$/.test(idCita)) notFound()
  return <ConsultaDoctor idCita={Number(idCita)} />
}
