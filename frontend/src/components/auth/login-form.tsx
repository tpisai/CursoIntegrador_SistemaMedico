"use client"

import { useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { CircleAlertIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card"
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldSeparator } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { RecuperarPinDialog } from "@/components/auth/recuperar-pin-dialog"
import { RegistroDialog } from "@/components/auth/registro-dialog"
import { iniciarSesion } from "@/lib/api/auth"
import { mensajeDeError } from "@/lib/api/http"
import type { Sesion } from "@/lib/api/types"
import { INICIO_POR_ROL, guardarSesion } from "@/lib/auth/acciones"
import { type ErroresDeCampo, erroresPorCampo, loginSchema, soloDigitos } from "@/lib/validation"

type DatosLogin = { dni: string; pin: string }
type Dialogo = "registro" | "recuperar" | null

export function LoginForm() {
  const router = useRouter()
  const [datos, setDatos] = useState<DatosLogin>({ dni: "", pin: "" })
  const [errores, setErrores] = useState<ErroresDeCampo<DatosLogin>>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [enviando, startTransition] = useTransition()

  function entrar(sesion: Sesion) {
    const destino = INICIO_POR_ROL[sesion.usuario.rol]
    if (!destino) {
      setErrorApi("Tu perfil aún no tiene un panel en esta versión del sistema.")
      return
    }
    guardarSesion(sesion)
    router.replace(destino)
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const resultado = loginSchema.safeParse(datos)
    if (!resultado.success) {
      setErrores(erroresPorCampo(resultado.error))
      return
    }
    setErrores({})
    setErrorApi(null)
    startTransition(async () => {
      try {
        entrar(await iniciarSesion(resultado.data))
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Card className="w-full max-w-[440px] shadow-[0_8px_28px_rgb(15_23_41/0.08)] [--card-spacing:--spacing(7)] sm:[--card-spacing:--spacing(9)]">
      <CardHeader>
        <h1 className="text-2xl font-bold">Iniciar sesión</h1>
        <CardDescription>Ingresa con tu DNI y tu PIN de 6 dígitos.</CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-5">
            <Field data-invalid={!!errores.dni}>
              <FieldLabel htmlFor="dni">DNI</FieldLabel>
              <Input
                id="dni"
                name="dni"
                inputMode="numeric"
                autoComplete="username"
                placeholder="Ej. 74521863"
                className="h-11"
                value={datos.dni}
                aria-invalid={!!errores.dni}
                onChange={(e) => setDatos((d) => ({ ...d, dni: soloDigitos(e.target.value, 8) }))}
              />
              <FieldError>{errores.dni}</FieldError>
            </Field>

            <Field data-invalid={!!errores.pin}>
              <FieldLabel htmlFor="pin">PIN</FieldLabel>
              <Input
                id="pin"
                name="pin"
                type="password"
                inputMode="numeric"
                autoComplete="current-password"
                placeholder="••••••"
                className="h-11"
                value={datos.pin}
                aria-invalid={!!errores.pin}
                onChange={(e) => setDatos((d) => ({ ...d, pin: soloDigitos(e.target.value, 6) }))}
              />
              {errores.pin ? (
                <FieldError>{errores.pin}</FieldError>
              ) : (
                <FieldDescription>PIN numérico de 6 dígitos</FieldDescription>
              )}
            </Field>

            <Button
              type="button"
              variant="link"
              className="h-auto justify-start p-0 text-left whitespace-normal"
              onClick={() => setDialogo("recuperar")}
            >
              ¿Olvidaste tu PIN? Recupéralo por correo electrónico
            </Button>

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="w-full" disabled={enviando}>
              {enviando ? <Spinner data-icon="inline-start" /> : null}
              Ingresar
            </Button>

            <FieldSeparator>¿Aún no tienes cuenta?</FieldSeparator>

            <Field>
              <Button
                type="button"
                variant="soft"
                size="xl"
                className="w-full"
                onClick={() => setDialogo("registro")}
              >
                Crear una cuenta
              </Button>
              <FieldDescription className="text-xs">
                Para pacientes y personal médico. Al registrarte deberás aceptar los Términos y Condiciones y
                la política de protección de datos (Ley N.° 29733).
              </FieldDescription>
            </Field>
          </FieldGroup>
        </form>
      </CardContent>

      {/* Fuera del <form>: los eventos de React atraviesan los portales y dispararían el login. */}
      <RegistroDialog
        open={dialogo === "registro"}
        onOpenChange={(abierto) => setDialogo(abierto ? "registro" : null)}
        onRegistrado={entrar}
      />
      <RecuperarPinDialog
        open={dialogo === "recuperar"}
        onOpenChange={(abierto) => setDialogo(abierto ? "recuperar" : null)}
      />
    </Card>
  )
}
