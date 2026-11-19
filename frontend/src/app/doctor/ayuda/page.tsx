import type { Metadata } from "next"

import { CentroAyuda } from "@/components/ayuda/centro-ayuda"

export const metadata: Metadata = {
  title: "Ayuda",
}

export default function AyudaDoctorPage() {
  return <CentroAyuda perfil="doctor" />
}
