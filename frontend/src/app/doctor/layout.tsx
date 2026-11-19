import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  { href: "/doctor", texto: "Inicio", exacto: true },
  // La página de consulta se abre desde la agenda, así que la mantiene resaltada.
  { href: "/doctor/agenda", texto: "Agenda", prefijos: ["/doctor/consulta"] },
  { href: "/doctor/solicitudes", texto: "Solicitudes" },
  { href: "/doctor/ayuda", texto: "Ayuda" },
]

export default function DoctorLayout({ children }: LayoutProps<"/doctor">) {
  return (
    <PortalShell rol="DOCTOR" enlaces={ENLACES} etiquetaRol="Doctor">
      {children}
    </PortalShell>
  )
}
