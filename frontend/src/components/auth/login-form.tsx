"use client"

import { useEffect, useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { CircleAlertIcon, EyeIcon, EyeOffIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field"
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

// "Recordar" guarda solo el DNI en este navegador; el PIN nunca se almacena.
const CLAVE_DNI_RECORDADO = "saludgrau:dni-recordado"

function leerDniRecordado() {
  try {
    return window.localStorage.getItem(CLAVE_DNI_RECORDADO)
  } catch {
    return null
  }
}

function guardarDniRecordado(dni: string | null) {
  try {
    if (dni) window.localStorage.setItem(CLAVE_DNI_RECORDADO, dni)
    else window.localStorage.removeItem(CLAVE_DNI_RECORDADO)
  } catch {
    // Almacenamiento bloqueado: simplemente no se recuerda.
  }
}

export function LoginForm() {
  const router = useRouter()
  const [datos, setDatos] = useState<DatosLogin>({ dni: "", pin: "" })
  const [recordar, setRecordar] = useState(false)
  const [verPin, setVerPin] = useState(false)
  const [errores, setErrores] = useState<ErroresDeCampo<DatosLogin>>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [enviando, startTransition] = useTransition()

  useEffect(() => {
    const dni = leerDniRecordado()
    if (dni) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- se lee localStorage solo tras hidratar.
      setDatos((d) => ({ ...d, dni }))
      setRecordar(true)
    }
  }, [])

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
        const sesion = await iniciarSesion(resultado.data)
        guardarDniRecordado(recordar ? resultado.data.dni : null)
        entrar(sesion)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  const completo = datos.dni.length === 8 && datos.pin.length === 6

  return (
    <div className="flex w-full max-w-[360px] flex-col gap-8">
      <div className="flex flex-col gap-2 text-center">
        <h1 className="text-[34px] leading-tight font-medium">Inicia sesión</h1>
        <p className="text-sm text-muted-foreground">Ingresa con tu DNI y tu PIN de 6 dígitos.</p>
      </div>

      <form onSubmit={onSubmit} noValidate>
        <FieldGroup className="gap-5">
          <Field data-invalid={!!errores.dni}>
            <FieldLabel htmlFor="dni" className="font-normal">
              DNI
            </FieldLabel>
            <Input
              id="dni"
              name="dni"
              inputMode="numeric"
              autoComplete="username"
              placeholder="Número de DNI"
              className="h-12 rounded-lg px-4 text-base"
              value={datos.dni}
              aria-invalid={!!errores.dni}
              onChange={(e) => setDatos((d) => ({ ...d, dni: soloDigitos(e.target.value, 8) }))}
            />
            <FieldError>{errores.dni}</FieldError>
          </Field>

          <Field data-invalid={!!errores.pin}>
            <FieldLabel htmlFor="pin" className="font-normal">
              PIN
            </FieldLabel>
            <div className="relative">
              <Input
                id="pin"
                name="pin"
                type={verPin ? "text" : "password"}
                inputMode="numeric"
                autoComplete="current-password"
                placeholder="Ingresa tu PIN"
                className="h-12 rounded-lg pr-12 pl-4 text-base"
                value={datos.pin}
                aria-invalid={!!errores.pin}
                onChange={(e) => setDatos((d) => ({ ...d, pin: soloDigitos(e.target.value, 6) }))}
              />
              <button
                type="button"
                onClick={() => setVerPin((v) => !v)}
                aria-pressed={verPin}
                className="absolute top-1/2 right-2 flex size-9 -translate-y-1/2 items-center justify-center rounded-md text-primary outline-none hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                {verPin ? <EyeOffIcon className="size-5" aria-hidden="true" /> : <EyeIcon className="size-5" aria-hidden="true" />}
                <span className="sr-only">{verPin ? "Ocultar PIN" : "Mostrar PIN"}</span>
              </button>
            </div>
            <FieldError>{errores.pin}</FieldError>
          </Field>

          <div className="flex items-center justify-between gap-3">
            <label className="flex items-center gap-2.5 text-sm">
              <Checkbox
                checked={recordar}
                onCheckedChange={(v) => setRecordar(v === true)}
                className="size-5"
              />
              Recordar DNI
            </label>
            <Button
              type="button"
              variant="link"
              className="h-auto p-0 text-sm font-normal"
              onClick={() => setDialogo("recuperar")}
            >
              Olvidé mi PIN
            </Button>
          </div>

          {errorApi ? (
            <Alert variant="destructive">
              <CircleAlertIcon />
              <AlertDescription>{errorApi}</AlertDescription>
            </Alert>
          ) : null}

          <Button
            type="submit"
            size="xl"
            variant={completo ? "default" : "secondary"}
            className="w-full rounded-full text-base data-[variant=secondary]:bg-muted data-[variant=secondary]:text-muted-foreground"
            disabled={enviando}
          >
            {enviando ? <Spinner data-icon="inline-start" /> : null}
            Ingresar
          </Button>

          <p className="text-center text-sm">
            ¿No tienes cuenta?{" "}
            <Button
              type="button"
              variant="link"
              className="h-auto p-0 font-semibold"
              onClick={() => setDialogo("registro")}
            >
              Regístrate
            </Button>
          </p>

          <Button
            type="button"
            variant="lime"
            size="xl"
            className="h-auto w-full rounded-lg py-3 whitespace-normal"
            onClick={() => setDialogo("registro")}
          >
            Regístrate para reservar tus citas y ver tu historial médico
          </Button>
          <p className="text-center text-xs leading-relaxed text-muted-foreground">
            Para pacientes y personal médico. Al registrarte aceptas los Términos y Condiciones y la política de
            protección de datos (Ley N.° 29733).
          </p>
        </FieldGroup>
      </form>

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
    </div>
  )
}
