"use client"

import { useState, useTransition } from "react"
import { CircleAlertIcon, StethoscopeIcon, UserRoundIcon } from "lucide-react"
import { toast } from "sonner"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldError, FieldGroup, FieldLabel, FieldTitle } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Spinner } from "@/components/ui/spinner"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { useEspecialidades } from "@/hooks/use-citas"
import { registrarCuenta } from "@/lib/api/auth"
import { mensajeDeError } from "@/lib/api/http"
import type { Sesion, TipoCuenta } from "@/lib/api/types"
import { primerNombre } from "@/lib/format"
import { type ErroresDeCampo, erroresPorCampo, registroSchema, soloDigitos } from "@/lib/validation"

type DatosRegistro = {
  tipoCuenta: TipoCuenta
  nombres: string
  apellidos: string
  dni: string
  correo: string
  pin: string
  confirmarPin: string
  terminosAceptados: boolean
  idEspecialidad?: number
  cmp: string
}

const VACIO: DatosRegistro = {
  tipoCuenta: "PACIENTE",
  nombres: "",
  apellidos: "",
  dni: "",
  correo: "",
  pin: "",
  confirmarPin: "",
  terminosAceptados: false,
  cmp: "",
}

// Modal M1 del prototipo, ampliado para crear cuentas de paciente o de doctor.
export function RegistroDialog({
  open,
  onOpenChange,
  onRegistrado,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onRegistrado: (sesion: Sesion) => void
}) {
  const [datos, setDatos] = useState<DatosRegistro>(VACIO)
  const [errores, setErrores] = useState<ErroresDeCampo<DatosRegistro>>({})
  const [errorApi, setErrorApi] = useState<string | null>(null)
  const [enviando, startTransition] = useTransition()
  const especialidades = useEspecialidades()
  const esDoctor = datos.tipoCuenta === "DOCTOR"

  function actualizar<K extends keyof DatosRegistro>(campo: K, valor: DatosRegistro[K]) {
    setDatos((d) => ({ ...d, [campo]: valor }))
  }

  function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const resultado = registroSchema.safeParse(datos)
    if (!resultado.success) {
      setErrores(erroresPorCampo(resultado.error))
      return
    }
    setErrores({})
    setErrorApi(null)
    const { confirmarPin: _, idEspecialidad, cmp, ...comunes } = resultado.data
    const registro = esDoctor ? { ...comunes, idEspecialidad, cmp } : comunes
    startTransition(async () => {
      try {
        const sesion = await registrarCuenta(registro)
        toast.success(`Cuenta creada. ¡Te damos la bienvenida, ${primerNombre(sesion.usuario.nombres)}!`)
        setDatos(VACIO)
        onOpenChange(false)
        onRegistrado(sesion)
      } catch (error) {
        setErrorApi(mensajeDeError(error))
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100svh-2rem)] gap-5 overflow-y-auto sm:max-w-[470px] sm:p-8">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Crear cuenta</DialogTitle>
          <DialogDescription>
            {esDoctor
              ? "Crea tu cuenta de personal médico. Necesitas tu número de colegiatura (CMP)."
              : "Regístrate con tu DNI y tus datos personales."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} noValidate>
          <FieldGroup className="gap-4">
            <Field>
              <FieldTitle id="reg-tipo">Tipo de cuenta</FieldTitle>
              <ToggleGroup
                type="single"
                variant="slot"
                size="lg"
                spacing={2}
                aria-labelledby="reg-tipo"
                className="grid w-full grid-cols-2"
                value={datos.tipoCuenta}
                onValueChange={(valor) => valor && actualizar("tipoCuenta", valor as TipoCuenta)}
              >
                <ToggleGroupItem value="PACIENTE" className="w-full">
                  <UserRoundIcon data-icon="inline-start" />
                  Paciente
                </ToggleGroupItem>
                <ToggleGroupItem value="DOCTOR" className="w-full">
                  <StethoscopeIcon data-icon="inline-start" />
                  Doctor
                </ToggleGroupItem>
              </ToggleGroup>
            </Field>

            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.nombres}>
                <FieldLabel htmlFor="reg-nombres">Nombres</FieldLabel>
                <Input
                  id="reg-nombres"
                  autoComplete="given-name"
                  placeholder="Ej. Ana Lucía"
                  className="h-10"
                  value={datos.nombres}
                  aria-invalid={!!errores.nombres}
                  onChange={(e) => actualizar("nombres", e.target.value)}
                />
                <FieldError>{errores.nombres}</FieldError>
              </Field>
              <Field data-invalid={!!errores.apellidos}>
                <FieldLabel htmlFor="reg-apellidos">Apellidos</FieldLabel>
                <Input
                  id="reg-apellidos"
                  autoComplete="family-name"
                  placeholder="Ej. Quispe Rojas"
                  className="h-10"
                  value={datos.apellidos}
                  aria-invalid={!!errores.apellidos}
                  onChange={(e) => actualizar("apellidos", e.target.value)}
                />
                <FieldError>{errores.apellidos}</FieldError>
              </Field>
            </div>

            <Field data-invalid={!!errores.dni}>
              <FieldLabel htmlFor="reg-dni">DNI</FieldLabel>
              <Input
                id="reg-dni"
                inputMode="numeric"
                autoComplete="username"
                placeholder="Ej. 74521863"
                className="h-10"
                value={datos.dni}
                aria-invalid={!!errores.dni}
                onChange={(e) => actualizar("dni", soloDigitos(e.target.value, 8))}
              />
              <FieldError>{errores.dni}</FieldError>
            </Field>

            <Field data-invalid={!!errores.correo}>
              <FieldLabel htmlFor="reg-correo">Correo electrónico</FieldLabel>
              <Input
                id="reg-correo"
                type="email"
                autoComplete="email"
                placeholder="ejemplo@correo.com"
                className="h-10"
                value={datos.correo}
                aria-invalid={!!errores.correo}
                onChange={(e) => actualizar("correo", e.target.value)}
              />
              <FieldError>{errores.correo}</FieldError>
            </Field>

            {esDoctor ? (
              <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_140px]">
                <Field data-invalid={!!errores.idEspecialidad}>
                  <FieldLabel htmlFor="reg-especialidad">Especialidad</FieldLabel>
                  <Select
                    value={datos.idEspecialidad ? String(datos.idEspecialidad) : ""}
                    onValueChange={(valor) => actualizar("idEspecialidad", Number(valor))}
                    disabled={!especialidades.data?.length}
                  >
                    <SelectTrigger
                      id="reg-especialidad"
                      size="lg"
                      className="w-full"
                      aria-invalid={!!errores.idEspecialidad}
                    >
                      <SelectValue placeholder={especialidades.data ? "Elige una" : "Cargando…"} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectGroup>
                        {especialidades.data?.map((e) => (
                          <SelectItem key={e.idEspecialidad} value={String(e.idEspecialidad)}>
                            {e.nombre}
                          </SelectItem>
                        ))}
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                  <FieldError>{errores.idEspecialidad}</FieldError>
                </Field>
                <Field data-invalid={!!errores.cmp}>
                  <FieldLabel htmlFor="reg-cmp">N.° de CMP</FieldLabel>
                  <Input
                    id="reg-cmp"
                    inputMode="numeric"
                    placeholder="Ej. 045812"
                    className="h-10"
                    value={datos.cmp}
                    aria-invalid={!!errores.cmp}
                    onChange={(e) => actualizar("cmp", soloDigitos(e.target.value, 6))}
                  />
                  <FieldError>{errores.cmp}</FieldError>
                </Field>
              </div>
            ) : null}

            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!errores.pin}>
                <FieldLabel htmlFor="reg-pin">PIN (6 dígitos)</FieldLabel>
                <Input
                  id="reg-pin"
                  type="password"
                  inputMode="numeric"
                  autoComplete="new-password"
                  placeholder="••••••"
                  className="h-10"
                  value={datos.pin}
                  aria-invalid={!!errores.pin}
                  onChange={(e) => actualizar("pin", soloDigitos(e.target.value, 6))}
                />
                <FieldError>{errores.pin}</FieldError>
              </Field>
              <Field data-invalid={!!errores.confirmarPin}>
                <FieldLabel htmlFor="reg-confirmar">Confirmar PIN</FieldLabel>
                <Input
                  id="reg-confirmar"
                  type="password"
                  inputMode="numeric"
                  autoComplete="new-password"
                  placeholder="••••••"
                  className="h-10"
                  value={datos.confirmarPin}
                  aria-invalid={!!errores.confirmarPin}
                  onChange={(e) => actualizar("confirmarPin", soloDigitos(e.target.value, 6))}
                />
                <FieldError>{errores.confirmarPin}</FieldError>
              </Field>
            </div>

            <Field orientation="horizontal" data-invalid={!!errores.terminosAceptados}>
              <Checkbox
                id="reg-terminos"
                checked={datos.terminosAceptados}
                aria-invalid={!!errores.terminosAceptados}
                onCheckedChange={(valor) => actualizar("terminosAceptados", valor === true)}
              />
              <div className="flex flex-col gap-1">
                <FieldLabel htmlFor="reg-terminos" className="leading-snug font-normal">
                  Acepto los términos y condiciones y la política de protección de datos personales (Ley N.°
                  29733).
                </FieldLabel>
                <FieldError>{errores.terminosAceptados}</FieldError>
              </div>
            </Field>

            {errorApi ? (
              <Alert variant="destructive">
                <CircleAlertIcon />
                <AlertDescription>{errorApi}</AlertDescription>
              </Alert>
            ) : null}

            <Button type="submit" size="xl" className="w-full" disabled={enviando}>
              {enviando ? <Spinner data-icon="inline-start" /> : null}
              {esDoctor ? "Crear cuenta de doctor" : "Crear cuenta"}
            </Button>
          </FieldGroup>
        </form>
      </DialogContent>
    </Dialog>
  )
}
