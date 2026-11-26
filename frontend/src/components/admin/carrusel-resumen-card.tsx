"use client"

import Link from "next/link"
import { ArrowRightIcon, GalleryHorizontalEndIcon, ImageIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAnunciosAdmin } from "@/hooks/use-admin"
import { BANNER, srcImagenAnuncio } from "@/lib/api/anuncios"

/** Acceso al carrusel de eventos con las miniaturas de los banners vigentes. */
export function CarruselResumenCard() {
  const { data, isLoading } = useAnunciosAdmin()
  const vigentes = data?.filter((a) => a.estado === "VIGENTE") ?? []

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 font-semibold">
          <GalleryHorizontalEndIcon className="size-5 text-primary" aria-hidden="true" />
          Carrusel de eventos
        </CardTitle>
        <CardDescription>
          Banners que hoy ven los pacientes ({BANNER.ancho} × {BANNER.alto} px).
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {isLoading ? (
          <Skeleton className="aspect-[15/8] w-full rounded-xl" />
        ) : vigentes.length ? (
          <ul className="grid grid-cols-2 gap-2">
            {vigentes.slice(0, 4).map((a) => (
              <li key={a.idAnuncio} className="relative aspect-[15/8] overflow-hidden rounded-lg bg-muted ring-1 ring-foreground/10">
                {/* eslint-disable-next-line @next/next/no-img-element -- la sirve el backend */}
                <img src={srcImagenAnuncio(a.imagenUrl)} alt={a.titulo} className="size-full object-cover" />
              </li>
            ))}
          </ul>
        ) : (
          <div className="flex aspect-[15/8] w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed bg-muted/40 p-4 text-center">
            <ImageIcon className="size-8 text-muted-foreground" aria-hidden="true" />
            <p className="text-sm text-muted-foreground">
              Sin banners propios: el portal muestra los anuncios institucionales.
            </p>
          </div>
        )}
        <Button asChild variant="outline" size="lg" className="w-full">
          <Link href="/admin/anuncios">
            {vigentes.length ? `Gestionar ${vigentes.length} banner${vigentes.length === 1 ? "" : "s"}` : "Publicar un banner"}
            <ArrowRightIcon data-icon="inline-end" />
          </Link>
        </Button>
      </CardContent>
    </Card>
  )
}
