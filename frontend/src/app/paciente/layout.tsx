import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  { href: "/paciente", texto: "Inicio", exacto: true },
  { href: "/paciente/citas", texto: "Mis citas" },
  { href: "/paciente/historial", texto: "Historial médico" },
  { href: "/paciente/comunicacion", texto: "Campañas" },
  { href: "/paciente/ayuda", texto: "Ayuda" },
]

export default function PacienteLayout({ children }: LayoutProps<"/paciente">) {
  return (
    <PortalShell rol="PACIENTE" enlaces={ENLACES}>
      {children}
    </PortalShell>
  )
}
