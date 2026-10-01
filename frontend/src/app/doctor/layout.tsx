import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  { href: "/doctor", texto: "Inicio", exacto: true },
  { href: "/doctor/agenda", texto: "Mi agenda" },
  { href: "/doctor/solicitudes", texto: "Solicitudes" },
]

export default function DoctorLayout({ children }: LayoutProps<"/doctor">) {
  return (
    <PortalShell rol="DOCTOR" enlaces={ENLACES} etiquetaRol="Doctor">
      {children}
    </PortalShell>
  )
}
