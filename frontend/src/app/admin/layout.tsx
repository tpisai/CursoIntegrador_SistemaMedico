import { PortalShell } from "@/components/portal/portal-shell"

const ENLACES = [
  { href: "/admin", texto: "Inicio", exacto: true },
  { href: "/admin/comunicacion", texto: "Campañas" },
  { href: "/admin/anuncios", texto: "Carrusel de eventos" },
  { href: "/admin/reportes", texto: "Reportes" },
]

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <PortalShell rol="ADMINISTRADOR" enlaces={ENLACES} etiquetaRol="Administración">
      {children}
    </PortalShell>
  )
}
