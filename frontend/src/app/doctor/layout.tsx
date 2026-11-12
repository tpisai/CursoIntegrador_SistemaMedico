import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  // La página de consulta se abre desde la agenda, así que la mantiene resaltada.
  { href: "/doctor/agenda", texto: "Agenda", prefijos: ["/doctor/consulta"] },
  { href: "/doctor/solicitudes", texto: "Solicitudes" },
]

export default function DoctorLayout({ children }: LayoutProps<"/doctor">) {
  return (
    <PortalShell rol="DOCTOR" enlaces={ENLACES} etiquetaRol="Doctor">
      {children}
    </PortalShell>
  )
}
