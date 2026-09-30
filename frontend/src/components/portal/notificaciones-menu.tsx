"use client"

import { BellIcon, BellOffIcon } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Popover, PopoverContent, PopoverTitle, PopoverTrigger } from "@/components/ui/popover"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { useNotificaciones } from "@/hooks/use-notificaciones"
import { mensajeDeError } from "@/lib/api/http"
import { marcarNotificacionLeida, marcarTodasLeidas } from "@/lib/api/notificaciones"
import type { Notificacion } from "@/lib/api/types"
import { tiempoRelativo } from "@/lib/format"
import { cn } from "@/lib/utils"

// Campana de la barra superior (RF-17, RF-22). Lee la tabla notificacion del usuario en sesión.
export function NotificacionesMenu() {
  const { data: notificaciones, isLoading, mutate } = useNotificaciones()
  const noLeidas = notificaciones?.filter((n) => !n.leida).length ?? 0

  async function leer(notificacion: Notificacion) {
    if (notificacion.leida) return
    const marcar = (lista: Notificacion[] = []) =>
      lista.map((n) => (n.idNotificacion === notificacion.idNotificacion ? { ...n, leida: true } : n))
    try {
      await mutate(async (lista) => {
        await marcarNotificacionLeida(notificacion.idNotificacion)
        return marcar(lista)
      }, { optimisticData: marcar, rollbackOnError: true, revalidate: false })
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  async function leerTodas() {
    const marcar = (lista: Notificacion[] = []) => lista.map((n) => ({ ...n, leida: true }))
    try {
      await mutate(async (lista) => {
        await marcarTodasLeidas()
        return marcar(lista)
      }, { optimisticData: marcar, rollbackOnError: true, revalidate: false })
    } catch (error) {
      toast.error(mensajeDeError(error))
    }
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          className="relative flex size-10 items-center justify-center rounded-full outline-none hover:bg-white/10 focus-visible:ring-3 focus-visible:ring-white/50"
        >
          <BellIcon className="size-5" aria-hidden="true" />
          {noLeidas > 0 ? (
            <span
              aria-hidden="true"
              className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] leading-none font-bold text-white"
            >
              {noLeidas > 9 ? "9+" : noLeidas}
            </span>
          ) : null}
          <span className="sr-only">
            {noLeidas > 0 ? `Notificaciones, ${noLeidas} sin leer` : "Notificaciones"}
          </span>
        </button>
      </PopoverTrigger>

      <PopoverContent align="end" className="w-[min(24rem,calc(100vw-2rem))] gap-0 p-0">
        <div className="flex items-center justify-between gap-2 px-4 py-3">
          <PopoverTitle className="font-semibold">Notificaciones</PopoverTitle>
          <Button variant="link" size="sm" className="h-auto p-0" disabled={noLeidas === 0} onClick={leerTodas}>
            Marcar todas como leídas
          </Button>
        </div>
        <Separator />

        {isLoading ? (
          <div className="flex flex-col gap-2 p-4">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-14" />
            ))}
          </div>
        ) : notificaciones?.length ? (
          <ul className="max-h-96 overflow-y-auto overscroll-contain">
            {notificaciones.map((n) => (
              <li key={n.idNotificacion} className="border-b last:border-b-0">
                <button
                  type="button"
                  onClick={() => leer(n)}
                  className={cn(
                    "flex w-full gap-3 px-4 py-3 text-left outline-none hover:bg-muted focus-visible:bg-muted",
                    !n.leida && "bg-secondary/60"
                  )}
                >
                  <span
                    className={cn("mt-1.5 size-2 shrink-0 rounded-full", n.leida ? "bg-transparent" : "bg-primary")}
                    aria-hidden="true"
                  />
                  <span className="flex min-w-0 flex-col gap-0.5">
                    <span className={cn("text-sm", !n.leida && "font-semibold")}>
                      {n.titulo}
                      {!n.leida ? <span className="sr-only"> (sin leer)</span> : null}
                    </span>
                    <span className="text-xs leading-relaxed text-muted-foreground">{n.mensaje}</span>
                    <span className="text-[11px] text-muted-foreground">{tiempoRelativo(n.fechaEnvio)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <Empty className="p-8">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <BellOffIcon />
              </EmptyMedia>
              <EmptyTitle>Sin notificaciones</EmptyTitle>
              <EmptyDescription>Aquí verás avisos de tus citas y solicitudes.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
      </PopoverContent>
    </Popover>
  )
}
