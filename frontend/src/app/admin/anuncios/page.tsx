import type { Metadata } from "next"

import { AnunciosAdmin } from "@/components/admin/anuncios-admin"

export const metadata: Metadata = {
  title: "Carrusel de eventos",
}

export default function AnunciosAdminPage() {
  return <AnunciosAdmin />
}
