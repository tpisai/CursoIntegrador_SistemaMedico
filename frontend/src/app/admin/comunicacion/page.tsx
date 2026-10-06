import type { Metadata } from "next"

import { ComunicacionAdmin } from "@/components/admin/comunicacion-admin"

export const metadata: Metadata = {
  title: "Comunicación",
}

export default function ComunicacionAdminPage() {
  return <ComunicacionAdmin />
}
