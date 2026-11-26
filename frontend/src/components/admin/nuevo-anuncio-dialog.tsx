"use client"

import { useEffect, useRef, useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { CircleAlertIcon, CircleCheckIcon, ImagePlusIcon, RulerIcon, Trash2Icon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Spinner } from "@/components/ui/spinner"
import { BannerImagen } from "@/components/anuncios/carrusel-eventos"
import { crearAnuncio, rutasAdmin } from "@/lib/api/admin"
import { BANNER, problemaMedidas, RUTA_ANUNCIOS } from "@/lib/api/anuncios"
import { mensajeDeError } from "@/lib/api/http"
import { toFechaISO } from "@/lib/format"
import { cn } from "@/lib/utils"

/** Destinos frecuentes del botón del banner; "externo" pide una URL. */
const DESTINOS = [
  { valor: "ninguno", texto: "Sin botón" },
  { valor: "/paciente/agendar", texto: "Agendar cita" },
  { valor: "/paciente/comunicacion", texto: "Campañas y eventos" },
  { valor: "/paciente/historial", texto: "Historial médico" },
  { valor: "/paciente/citas", texto: "Mis citas" },
  { valor: "/paciente/ayuda", texto: "Centro de ayuda" },
  { valor: "externo", texto: "Enlace externo (https://…)" },
]

type Imagen = { archivo: File; url: string; ancho: number; alto: number }

type Datos = {
  titulo: string
  etiqueta: string
  descripcion: string
  destino: string
  enlaceExterno: string
  textoBoton: string
  fechaInicio: string
  fechaFin: string
}
type Errores = Partial<Record<keyof Datos | "imagen", string>>

function datosIniciales(): Datos {
  const hoy = new Date()
  const fin = new Date(hoy)
  fin.setDate(fin.getDate() + 30)
  return {
    titulo: "",
    etiqueta: "",
    descripcion: "",
    destino: "ninguno",
    enlaceExterno: "",
    textoBoton: "",
    fechaInicio: toFechaISO(hoy),
    fechaFin: toFechaISO(fin),
  }
}

function leerMedidas(url: string) {
  return new Promise<{ ancho: number; alto: number }>((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve({ ancho: img.naturalWidth, alto: img.naturalHeight })
    img.onerror = () => reject(new Error("imagen"))
    img.src = url
  })
}

export function NuevoAnuncioDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { mutate } = useSWRConfig()
  const [hoy] = useState(() => toFechaISO(new Date()))
  const [datos, setDatos] = useState<Datos>(datosIniciales)
  const [imagen, setImagen] = useState<Imagen | null>(null)
  const [errores, setErrores] = useState<Errores>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [arrastrando, setArrastrando] = useState(false)
  const [publicando, startTransition] = useTransition()
  const inputArchivo = useRef<HTMLInputElement>(null)

  // Libera la URL temporal de la imagen anterior.
  useEffect(() => () => (imagen ? URL.revokeObjectURL(imagen.url) : undefined), [imagen])

  function actualizar(cambios: Partial<Datos>) {
    setDatos((d) => ({ ...d, ...cambios }))
    setErrores((e) => {
      const restantes = { ...e }
      for (const campo of Object.keys(cambios) as (keyof Datos)[]) delete restantes[campo]
      return restantes
    })
  }

  function reiniciar() {
    setDatos(datosIniciales())
    setImagen(null)
    setErrores({})
    setErrorApi(null)
  }

  function cambiarApertura(abierto: boolean) {
    if (!abierto) {
      setErrores({})
      setErrorApi(null)
    }
    onOpenChange(abierto)
  }

  async function elegirArchivo(archivo: File | undefined) {
    if (!archivo) return
    setErrores((e) => ({ ...e, imagen: undefined }))
    if (!["image/jpeg", "image/png"].includes(archivo.type)) {
      setErrores((e) => ({ ...e, imagen: "La imagen debe ser JPG o PNG." }))
      return
    }
    if (archivo.size > BANNER.maxBytes) {
      setErrores((e) => ({ ...e, imagen: "La imagen supera el máximo de 5 MB. Comprímela e inténtalo de nuevo." }))
      return
    }
    const url = URL.createObjectURL(archivo)
    try {
      const { ancho, alto } = await leerMedidas(url)
      const problema = problemaMedidas(ancho, alto)
      if (problema) {
        URL.revokeObjectURL(url)
        setErrores((e) => ({ ...e, imagen: problema }))
        return
      }
      setImagen({ archivo, url, ancho, alto })
    } catch {
      URL.revokeObjectURL(url)
      setErrores((e) => ({ ...e, imagen: "No pudimos leer la imagen. Prueba con otro archivo." }))
    }
  }

  function enlaceFinal() {
    if (datos.destino === "ninguno") return undefined
    if (datos.destino === "externo") return datos.enlaceExterno.trim() || undefined
    return datos.destino
  }

  function validar(): Errores {
    const e: Errores = {}
    if (datos.titulo.trim().length < 5) e.titulo = "Escribe un título de al menos 5 caracteres."
    if (!imagen) e.imagen = `Sube la imagen del banner (${BANNER.ancho} × ${BANNER.alto} px).`
    if (!datos.fechaInicio) e.fechaInicio = "Indica desde cuándo se muestra."
    if (!datos.fechaFin) e.fechaFin = "Indica hasta cuándo se muestra."
    else if (datos.fechaFin < hoy) e.fechaFin = "No puede terminar en el pasado."
    else if (datos.fechaInicio && datos.fechaFin < datos.fechaInicio) e.fechaFin = "Debe ser igual o posterior al inicio."
    if (datos.destino === "externo" && !/^https?:\/\/\S+$/.test(datos.enlaceExterno.trim())) {
      e.enlaceExterno = "Escribe una dirección que empiece con https://"
    }
    return e
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nuevos = validar()
    setErrores(nuevos)
    if (Object.keys(nuevos).length || !imagen) return
    setErrorApi(null)
    startTransition(async () => {
      try {
        const enlace = enlaceFinal()
        const anuncio = await crearAnuncio({
          titulo: datos.titulo.trim(),
          etiqueta: datos.etiqueta.trim() || undefined,
          descripcion: datos.descripcion.trim() || undefined,
          enlace,
          textoBoton: enlace ? datos.textoBoton.trim() || undefined : undefined,
          fechaInicio: datos.fechaInicio,
          fechaFin: datos.fechaFin,
          imagen: imagen.archivo,
        })
        toast.success(
          anuncio.estado === "VIGENTE"
            ? `"${anuncio.titulo}" ya aparece en el carrusel de los pacientes.`
            : `"${anuncio.titulo}" se mostrará desde el ${anuncio.fechaInicio.split("-").reverse().join("/")}.`
        )
        await Promise.all([mutate(rutasAdmin.anuncios), mutate(RUTA_ANUNCIOS)])
        reiniciar()
        onOpenChange(false)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  const enlace = enlaceFinal()

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent className="max-h-[calc(100svh-2rem)] gap-5 overflow-y-auto sm:max-w-[960px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Nuevo banner del carrusel</DialogTitle>
          <DialogDescription>
            Aparecerá en Campañas y en el inicio del portal del paciente durante las fechas que elijas.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} noValidate className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)]">
          {/* Columna derecha en escritorio: imagen y vista previa */}
          <div className="flex flex-col gap-3 lg:order-2">
            <Field data-invalid={!!errores.imagen}>
              <FieldLabel>Imagen del banner</FieldLabel>
              {imagen ? (
                <div className="flex flex-col gap-2">
                  <div className="@container relative aspect-[15/8] w-full overflow-hidden rounded-2xl bg-navy ring-1 ring-foreground/10">
                    <BannerImagen
                      vistaPrevia
                      src={imagen.url}
                      titulo={datos.titulo.trim() || "Título del banner"}
                      descripcion={datos.descripcion.trim() || null}
                      etiqueta={datos.etiqueta.trim() || null}
                      enlace={enlace ?? null}
                      textoBoton={datos.textoBoton.trim() || "Más información"}
                    />
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span className="flex items-center gap-1.5 font-medium text-success-foreground">
                      <CircleCheckIcon className="size-4" aria-hidden="true" />
                      {imagen.ancho} × {imagen.alto} px · {(imagen.archivo.size / 1024 / 1024).toFixed(1)} MB
                    </span>
                    <Button type="button" variant="ghost" size="sm" onClick={() => setImagen(null)}>
                      <Trash2Icon data-icon="inline-start" />
                      Cambiar imagen
                    </Button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => inputArchivo.current?.click()}
                  onDragOver={(e) => {
                    e.preventDefault()
                    setArrastrando(true)
                  }}
                  onDragLeave={() => setArrastrando(false)}
                  onDrop={(e) => {
                    e.preventDefault()
                    setArrastrando(false)
                    void elegirArchivo(e.dataTransfer.files[0])
                  }}
                  aria-describedby={errores.imagen ? "anuncio-imagen-error" : undefined}
                  className={cn(
                    "flex aspect-[15/8] w-full flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed bg-muted/40 p-6 text-center outline-none transition-colors hover:border-primary hover:bg-secondary focus-visible:ring-3 focus-visible:ring-ring/50",
                    errores.imagen && "border-destructive",
                    arrastrando && "border-primary bg-secondary"
                  )}
                >
                  <span className="flex size-14 items-center justify-center rounded-2xl bg-card text-primary shadow-sm">
                    <ImagePlusIcon className="size-7" aria-hidden="true" />
                  </span>
                  <span className="text-sm font-semibold">Arrastra la imagen aquí o haz clic para elegirla</span>
                  <span className="flex items-center gap-1.5 rounded-full bg-card px-3 py-1 text-xs font-semibold text-primary ring-1 ring-primary/20">
                    <RulerIcon className="size-3.5" aria-hidden="true" />
                    {BANNER.ancho} × {BANNER.alto} px · JPG o PNG · máx. 5 MB
                  </span>
                </button>
              )}
              <input
                ref={inputArchivo}
                type="file"
                accept="image/jpeg,image/png"
                className="sr-only"
                tabIndex={-1}
                onChange={(e) => {
                  void elegirArchivo(e.target.files?.[0])
                  e.target.value = ""
                }}
              />
              {errores.imagen ? (
                <FieldError id="anuncio-imagen-error">{errores.imagen}</FieldError>
              ) : (
                <FieldDescription>
                  Rectangular 15:8. Deja libre el tercio inferior izquierdo: ahí el portal escribe el título y el botón.
                </FieldDescription>
              )}
            </Field>
          </div>

          <FieldGroup className="gap-4 lg:order-1">
            <Field data-invalid={!!errores.titulo}>
              <FieldLabel htmlFor="anuncio-titulo">Título</FieldLabel>
              <Input
                id="anuncio-titulo"
                placeholder="Ej. Abrimos el servicio de Ecografía"
                maxLength={120}
                className="h-11"
                aria-invalid={!!errores.titulo}
                value={datos.titulo}
                onChange={(e) => actualizar({ titulo: e.target.value })}
              />
              <FieldError>{errores.titulo}</FieldError>
            </Field>
            <div className="grid gap-4 sm:grid-cols-[180px_minmax(0,1fr)]">
              <Field>
                <FieldLabel htmlFor="anuncio-etiqueta">Etiqueta</FieldLabel>
                <Input
                  id="anuncio-etiqueta"
                  placeholder="Ej. Nuevo servicio"
                  maxLength={40}
                  className="h-11"
                  value={datos.etiqueta}
                  onChange={(e) => actualizar({ etiqueta: e.target.value })}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="anuncio-descripcion">Descripción corta</FieldLabel>
                <Input
                  id="anuncio-descripcion"
                  placeholder="Una línea que invite a participar"
                  maxLength={300}
                  className="h-11"
                  value={datos.descripcion}
                  onChange={(e) => actualizar({ descripcion: e.target.value })}
                />
              </Field>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="anuncio-destino">Botón lleva a</FieldLabel>
                <Select value={datos.destino} onValueChange={(v) => actualizar({ destino: v })}>
                  <SelectTrigger id="anuncio-destino" size="lg" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      {DESTINOS.map((d) => (
                        <SelectItem key={d.valor} value={d.valor}>
                          {d.texto}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  </SelectContent>
                </Select>
              </Field>
              <Field>
                <FieldLabel htmlFor="anuncio-boton">Texto del botón</FieldLabel>
                <Input
                  id="anuncio-boton"
                  placeholder="Más información"
                  maxLength={40}
                  className="h-11"
                  disabled={datos.destino === "ninguno"}
                  value={datos.textoBoton}
                  onChange={(e) => actualizar({ textoBoton: e.target.value })}
                />
              </Field>
            </div>
            {datos.destino === "externo" ? (
              <Field data-invalid={!!errores.enlaceExterno}>
                <FieldLabel htmlFor="anuncio-enlace">Dirección web</FieldLabel>
                <Input
                  id="anuncio-enlace"
                  type="url"
                  placeholder="https://www.gob.pe/minsa"
                  maxLength={255}
                  className="h-11"
                  aria-invalid={!!errores.enlaceExterno}
                  value={datos.enlaceExterno}
                  onChange={(e) => actualizar({ enlaceExterno: e.target.value })}
                />
                <FieldError>{errores.enlaceExterno}</FieldError>
              </Field>
            ) : null}

            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.fechaInicio}>
                <FieldLabel htmlFor="anuncio-inicio">Se muestra desde</FieldLabel>
                <Input
                  id="anuncio-inicio"
                  type="date"
                  className="h-11"
                  aria-invalid={!!errores.fechaInicio}
                  value={datos.fechaInicio}
                  onChange={(e) => actualizar({ fechaInicio: e.target.value })}
                />
                <FieldError>{errores.fechaInicio}</FieldError>
              </Field>
              <Field data-invalid={!!errores.fechaFin}>
                <FieldLabel htmlFor="anuncio-fin">Hasta</FieldLabel>
                <Input
                  id="anuncio-fin"
                  type="date"
                  min={datos.fechaInicio || hoy}
                  className="h-11"
                  aria-invalid={!!errores.fechaFin}
                  value={datos.fechaFin}
                  onChange={(e) => actualizar({ fechaFin: e.target.value })}
                />
                <FieldError>{errores.fechaFin}</FieldError>
              </Field>
            </div>

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="mt-1 w-full" disabled={publicando}>
              {publicando ? <Spinner data-icon="inline-start" /> : null}
              Publicar banner
            </Button>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
