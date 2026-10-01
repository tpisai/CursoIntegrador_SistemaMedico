"use client"

import { useState, useTransition } from "react"
import { CircleAlertIcon, MailCheckIcon } from "lucide-react"

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
import { recuperarPin } from "@/lib/api/auth"
import { mensajeDeError } from "@/lib/api/http"
import { recuperarPinSchema } from "@/lib/validation"

export function RecuperarPinDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [correo, setCorreo] = useState("")
  const [errorCampo, setErrorCampo] = useState<string | null>(null)
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [enviado, setEnviado] = useState(false)
  const [enviando, startTransition] = useTransition()

  function cambiarApertura(abierto: boolean) {
    if (!abierto) {
      setEnviado(false)
      setErrorApi(null)
      setErrorCampo(null)
    }
    onOpenChange(abierto)
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const resultado = recuperarPinSchema.safeParse({ correo })
    if (!resultado.success) {
      setErrorCampo(resultado.error.issues[0]?.message ?? null)
      return
    }
    setErrorCampo(null)
    setErrorApi(null)
    startTransition(async () => {
      try {
        await recuperarPin(resultado.data)
        setEnviado(true)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent className="gap-5 sm:max-w-[420px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Recuperar PIN</DialogTitle>
          <DialogDescription>
            Te enviaremos un código de verificación al correo electrónico registrado.
          </DialogDescription>
        </DialogHeader>

        {enviado ? (
          <div className="flex flex-col gap-5">
            <Alert variant="brand">
              <MailCheckIcon />
              <AlertTitle>Revisa tu bandeja de entrada</AlertTitle>
              <AlertDescription>
                Si {correo.trim()} está registrado, recibirás un código para crear un nuevo PIN. Puede
                tardar unos minutos.
              </AlertDescription>
            </Alert>
            <DialogClose asChild>
              <Button size="xl" className="w-full">
                Volver a iniciar sesión
              </Button>
            </DialogClose>
          </div>
        ) : (
          <form onSubmit={onSubmit} noValidate>
            <FieldGroup className="gap-5">
              <Field data-invalid={!!errorCampo}>
                <FieldLabel htmlFor="rec-correo">Correo electrónico</FieldLabel>
                <Input
                  id="rec-correo"
                  type="email"
                  autoComplete="email"
                  placeholder="ejemplo@correo.com"
                  className="h-10"
                  value={correo}
                  aria-invalid={!!errorCampo}
                  onChange={(e) => setCorreo(e.target.value)}
                />
                <FieldError>{errorCampo}</FieldError>
              </Field>

              {errorApi ? (
                <Alert variant="destructive">
                  <CircleAlertIcon />
                  <AlertDescription>{errorApi}</AlertDescription>
                </Alert>
              ) : null}

              <Button type="submit" size="xl" className="w-full" disabled={enviando}>
                {enviando ? <Spinner data-icon="inline-start" /> : null}
                Enviar código de verificación
              </Button>
            </FieldGroup>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
