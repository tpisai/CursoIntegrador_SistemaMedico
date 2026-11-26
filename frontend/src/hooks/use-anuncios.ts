"use client"

import useSWR from "swr"

import { RUTA_ANUNCIOS } from "@/lib/api/anuncios"
import type { Anuncio } from "@/lib/api/types"

/** Banners vigentes del carrusel de eventos. */
export function useAnuncios() {
  return useSWR<Anuncio[]>(RUTA_ANUNCIOS)
}
