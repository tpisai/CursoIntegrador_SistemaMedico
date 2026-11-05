"use client"

import { useState, useTransition } from "react"
import { CircleAlertIcon, CircleCheckIcon, MailCheckIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { recuperarPin, restablecerPin } from "@/lib/api/auth"
import { mensajeDeError } from "@/lib/api/http"
import {
  type ErroresDeCampo,
  erroresPorCampo,
  recuperarPinSchema,
  restablecerPinSchema,
  soloDigitos,
} from "@/lib/validation"

type Paso = "correo" | "codigo" | "listo"
type Codigo = { codigo: string; pinNuevo: string; confirmarPin: string }

const CODIGO_VACIO: Codigo = { codigo: "", pinNuevo: "", confirmarPin: "" }

const DESCRIPCION: Record<Paso, string> = {
  correo: "Te enviaremos un código de verificación al correo electrónico registrado.",
  codigo: "Escribe el código que te enviamos y elige tu nuevo PIN de 6 dígitos.",
  listo: "Ya puedes entrar con tu nuevo PIN.",
}

// RF-03: el código llega al correo registrado y vence a los 15 minutos.
export function RecuperarPinDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [paso, setPaso] = useState<Paso>("correo")
  const [correo, setCorreo] = useState("")
  const [datos, setDatos] = useState<Codigo>(CODIGO_VACIO)
  const [errorCorreo, setErrorCorreo] = useState<string | null>(null)
  const [errores, setErrores] = useState<ErroresDeCampo<Codigo>>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [enviando, startTransition] = useTransition()
  const [reenviando, startReenvio] = useTransition()

  function cambiarApertura(abierto: boolean) {
    if (!abierto) {
      setPaso("correo")
      setDatos(CODIGO_VACIO)
      setErrores({})
      setErrorApi(null)
      setErrorCorreo(null)
    }
    onOpenChange(abierto)
  }

  function actualizar(campo: keyof Codigo, valor: string) {
    setDatos((d) => ({ ...d, [campo]: soloDigitos(valor, 6) }))
    // El aviso de un campo desaparece en cuanto se corrige.
    setErrores((e) => ({ ...e, [campo]: undefined }))
  }

  function pedirCodigo(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const resultado = recuperarPinSchema.safeParse({ correo })
    if (!resultado.success) {
      setErrorCorreo(resultado.error.issues[0]?.message ?? null)
      return
    }
    setErrorCorreo(null)
    setErrorApi(null)
    startTransition(async () => {
      try {
        await recuperarPin(resultado.data)
        setPaso("codigo")
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  function reenviar() {
    setErrorApi(null)
    startReenvio(async () => {
      try {
        await recuperarPin({ correo: correo.trim() })
        toast.success("Si pasó más de un minuto, te enviamos un código nuevo.")
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  function cambiarPin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const resultado = restablecerPinSchema.safeParse(datos)
    if (!resultado.success) {
      setErrores(erroresPorCampo(resultado.error))
      return
    }
    setErrores({})
    setErrorApi(null)
    startTransition(async () => {
      try {
        const { codigo, pinNuevo } = resultado.data
        await restablecerPin({ correo: correo.trim(), codigo, pinNuevo })
        setPaso("listo")
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  const alertaError = errorApi ? (
    <Alert variant="destructive">
      <CircleAlertIcon />
      <AlertDescription>{errorApi}</AlertDescription>
    </Alert>
  ) : null

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent className="gap-5 sm:max-w-[420px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Recuperar PIN</DialogTitle>
          <DialogDescription>{DESCRIPCION[paso]}</DialogDescription>
        </DialogHeader>

        {paso === "correo" ? (
          <form onSubmit={pedirCodigo} noValidate>
            <FieldGroup className="gap-5">
              <Field data-invalid={!!errorCorreo}>
                <FieldLabel htmlFor="rec-correo">Correo electrónico</FieldLabel>
                <Input
                  id="rec-correo"
                  type="email"
                  autoComplete="email"
                  placeholder="ejemplo@correo.com"
                  className="h-11"
                  value={correo}
                  aria-invalid={!!errorCorreo}
                  onChange={(e) => {
                    setCorreo(e.target.value)
                    setErrorCorreo(null)
                  }}
                />
                <FieldError>{errorCorreo}</FieldError>
              </Field>

              {alertaError}

              <Button type="submit" size="xl" className="w-full" disabled={enviando}>
                {enviando ? <Spinner data-icon="inline-start" /> : null}
                Enviar código de verificación
              </Button>
            </FieldGroup>
          </form>
        ) : null}

        {paso === "codigo" ? (
          <form onSubmit={cambiarPin} noValidate>
            <FieldGroup className="gap-4">
              <Alert variant="brand">
                <MailCheckIcon />
                <AlertTitle>Revisa tu bandeja de entrada</AlertTitle>
                <AlertDescription>
                  Si {correo.trim()} está registrado, te llegará un código que vence en 15 minutos.
                </AlertDescription>
              </Alert>

              <Field data-invalid={!!errores.codigo}>
                <FieldLabel htmlFor="rec-codigo">Código de verificación</FieldLabel>
                <Input
                  id="rec-codigo"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  placeholder="6 dígitos"
                  className="h-11"
                  value={datos.codigo}
                  aria-invalid={!!errores.codigo}
                  onChange={(e) => actualizar("codigo", e.target.value)}
                />
                <FieldError>{errores.codigo}</FieldError>
              </Field>

              <div className="grid gap-4 sm:grid-cols-2">
                <Field data-invalid={!!errores.pinNuevo}>
                  <FieldLabel htmlFor="rec-pin">Nuevo PIN</FieldLabel>
                  <Input
                    id="rec-pin"
                    type="password"
                    inputMode="numeric"
                    autoComplete="new-password"
                    placeholder="6 dígitos"
                    className="h-11"
                    value={datos.pinNuevo}
                    aria-invalid={!!errores.pinNuevo}
                    onChange={(e) => actualizar("pinNuevo", e.target.value)}
                  />
                  <FieldError>{errores.pinNuevo}</FieldError>
                </Field>
                <Field data-invalid={!!errores.confirmarPin}>
                  <FieldLabel htmlFor="rec-confirmar">Repite el PIN</FieldLabel>
                  <Input
                    id="rec-confirmar"
                    type="password"
                    inputMode="numeric"
                    autoComplete="new-password"
                    placeholder="6 dígitos"
                    className="h-11"
                    value={datos.confirmarPin}
                    aria-invalid={!!errores.confirmarPin}
                    onChange={(e) => actualizar("confirmarPin", e.target.value)}
                  />
                  <FieldError>{errores.confirmarPin}</FieldError>
                </Field>
              </div>

              {alertaError}

              <div className="flex flex-col gap-2">
                <Button type="submit" size="xl" className="w-full" disabled={enviando}>
                  {enviando ? <Spinner data-icon="inline-start" /> : null}
                  Cambiar mi PIN
                </Button>
                <Button type="button" variant="link" disabled={reenviando} onClick={reenviar}>
                  {reenviando ? <Spinner data-icon="inline-start" /> : null}
                  No me llegó, reenviar código
                </Button>
              </div>
            </FieldGroup>
          </form>
        ) : null}

        {paso === "listo" ? (
          <div className="flex flex-col gap-5">
            <Alert variant="brand">
              <CircleCheckIcon />
              <AlertTitle>Tu PIN se actualizó</AlertTitle>
              <AlertDescription>Si tu cuenta estaba bloqueada, también quedó desbloqueada.</AlertDescription>
            </Alert>
            <DialogClose asChild>
              <Button size="xl" className="w-full">
                Volver a iniciar sesión
              </Button>
            </DialogClose>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
