import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  { href: "/admin", texto: "Inicio", exacto: true },
  { href: "/admin/comunicacion", texto: "Comunicación" },
]

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <PortalShell rol="ADMINISTRADOR" enlaces={ENLACES} etiquetaRol="Administración">
      {children}
    </PortalShell>
  )
}
