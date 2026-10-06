"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { LogOutIcon, MenuIcon } from "lucide-react"

import { Logo } from "@/components/brand/logo"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Separator } from "@/components/ui/separator"
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { NotificacionesMenu } from "@/components/portal/notificaciones-menu"
import type { Usuario } from "@/lib/api/types"
import { cerrarSesion } from "@/lib/auth/acciones"
import { iniciales } from "@/lib/format"

export type EnlacePortal = { href: string; texto: string; exacto?: boolean }

function esActivo(pathname: string, { href, exacto }: EnlacePortal) {
  return exacto ? pathname === href : pathname.startsWith(href)
}

export function PortalNavbar({
  usuario,
  enlaces,
  etiquetaRol,
}: {
  usuario: Usuario
  enlaces: EnlacePortal[]
  // Distintivo junto al logo, p. ej. "Doctor".
  etiquetaRol?: string
}) {
  const pathname = usePathname()
  const router = useRouter()
  const nombreCorto = `${usuario.nombres.split(" ")[0]} ${usuario.apellidos.split(" ")[0]}`
  const detalle = usuario.especialidad ?? `DNI ${usuario.dni}`

  async function salir() {
    await cerrarSesion()
    router.replace("/login")
  }

  return (
    <header className="sticky top-0 z-40 bg-brand-strong text-brand-foreground">
      <div className="mx-auto flex h-16 max-w-[1440px] items-center gap-3 px-4 sm:gap-6 sm:px-8 lg:h-[72px] lg:gap-8">
        <div className="flex items-center gap-3">
          <Link href={enlaces[0].href} className="rounded-md outline-none focus-visible:ring-3 focus-visible:ring-white/50">
            <Logo className="text-lg" markClassName="size-[30px]" />
          </Link>
          {etiquetaRol ? (
            // En móvil no entra junto a la campana y el menú; el menú lateral ya muestra el rol.
            <span className="hidden rounded-full bg-white/15 px-2.5 py-0.5 text-[11px] font-semibold tracking-wider uppercase sm:inline">
              {etiquetaRol}
            </span>
          ) : null}
        </div>

        <nav aria-label="Principal" className="hidden h-full lg:block">
          <ul className="flex h-full items-center gap-7">
            {enlaces.map((enlace) => (
              <li key={enlace.href} className="h-full">
                <Link
                  href={enlace.href}
                  aria-current={esActivo(pathname, enlace) ? "page" : undefined}
                  className="relative flex h-full items-center text-sm font-medium text-brand-foreground/75 outline-none after:absolute after:inset-x-0 after:bottom-0 after:h-[3px] after:rounded-t-full after:bg-brand-foreground after:opacity-0 hover:text-brand-foreground focus-visible:underline aria-[current=page]:font-semibold aria-[current=page]:text-brand-foreground aria-[current=page]:after:opacity-100"
                >
                  {enlace.texto}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          <NotificacionesMenu />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                className="flex items-center gap-2 rounded-full bg-white/10 p-1.5 text-sm outline-none hover:bg-white/15 focus-visible:ring-3 focus-visible:ring-white/50 sm:pr-3.5"
              >
                <Avatar size="sm" className="size-7 after:border-transparent">
                  <AvatarFallback className="bg-primary text-xs font-semibold text-primary-foreground">
                    {iniciales(usuario.nombres, usuario.apellidos)}
                  </AvatarFallback>
                </Avatar>
                <span className="hidden sm:inline">
                  {nombreCorto} · {detalle}
                </span>
                <span className="sr-only sm:hidden">Menú de la cuenta</span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-60">
              <DropdownMenuLabel className="flex flex-col gap-0.5">
                <span className="font-semibold text-foreground">
                  {usuario.nombres} {usuario.apellidos}
                </span>
                <span className="truncate font-normal">{usuario.correo}</span>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuGroup>
                <DropdownMenuItem variant="destructive" onSelect={salir}>
                  <LogOutIcon />
                  Cerrar sesión
                </DropdownMenuItem>
              </DropdownMenuGroup>
            </DropdownMenuContent>
          </DropdownMenu>

          <Sheet>
            <SheetTrigger asChild>
              <Button
                variant="ghost"
                size="icon-lg"
                className="text-brand-foreground hover:bg-white/10 hover:text-brand-foreground lg:hidden"
              >
                <MenuIcon />
                <span className="sr-only">Abrir menú</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-72">
              <SheetHeader>
                <SheetTitle>SaludGrau{etiquetaRol ? ` · ${etiquetaRol}` : ""}</SheetTitle>
                <SheetDescription>
                  {nombreCorto} · {detalle}
                </SheetDescription>
              </SheetHeader>
              <nav aria-label="Principal (móvil)" className="px-4">
                <ul className="flex flex-col gap-1">
                  {enlaces.map((enlace) => (
                    <li key={enlace.href}>
                      <SheetClose asChild>
                        <Link
                          href={enlace.href}
                          aria-current={esActivo(pathname, enlace) ? "page" : undefined}
                          className="flex h-10 items-center rounded-lg px-3 text-sm font-medium outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 aria-[current=page]:bg-accent aria-[current=page]:text-accent-foreground"
                        >
                          {enlace.texto}
                        </Link>
                      </SheetClose>
                    </li>
                  ))}
                </ul>
                <Separator className="my-3" />
                <Button variant="destructive" className="w-full justify-start" onClick={salir}>
                  <LogOutIcon data-icon="inline-start" />
                  Cerrar sesión
                </Button>
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
