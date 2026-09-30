import { z } from "zod"

const dni = z.string().trim().regex(/^\d{8}$/, "El DNI debe tener 8 dígitos.")
const pin = z.string().regex(/^\d{6}$/, "El PIN debe tener 6 dígitos numéricos.")
const correo = z.string().trim().pipe(z.email("Ingresa un correo electrónico válido."))
const nombre = (campo: string) =>
  z.string().trim().min(2, `Ingresa tus ${campo}.`).max(100, `Tus ${campo} son demasiado largos.`)

export const loginSchema = z.object({ dni, pin })

export const registroSchema = z
  .object({
    tipoCuenta: z.enum(["PACIENTE", "DOCTOR"]),
    nombres: nombre("nombres"),
    apellidos: nombre("apellidos"),
    dni,
    correo,
    pin,
    confirmarPin: z.string(),
    terminosAceptados: z.literal(true, {
      error: "Debes aceptar los términos y condiciones para continuar.",
    }),
    idEspecialidad: z.number().optional(),
    cmp: z.string().trim().optional(),
  })
  .superRefine((datos, ctx) => {
    if (datos.pin !== datos.confirmarPin) {
      ctx.addIssue({ code: "custom", path: ["confirmarPin"], message: "Los PIN no coinciden." })
    }
    if (datos.tipoCuenta !== "DOCTOR") return
    if (!datos.idEspecialidad) {
      ctx.addIssue({ code: "custom", path: ["idEspecialidad"], message: "Elige tu especialidad." })
    }
    if (!datos.cmp || !/^\d{5,6}$/.test(datos.cmp)) {
      ctx.addIssue({ code: "custom", path: ["cmp"], message: "El CMP debe tener 5 o 6 dígitos." })
    }
  })

export const recuperarPinSchema = z.object({ correo })

export type ErroresDeCampo<T> = Partial<Record<keyof T, string>>

/** Primer mensaje de error por campo, listo para mostrar debajo de cada input. */
export function erroresPorCampo<T>(error: z.ZodError<T>): ErroresDeCampo<T> {
  const errores: ErroresDeCampo<T> = {}
  for (const issue of error.issues) {
    const campo = issue.path[0] as keyof T | undefined
    if (campo !== undefined && !errores[campo]) errores[campo] = issue.message
  }
  return errores
}

/** Deja solo dígitos y corta al largo máximo (para inputs de DNI y PIN). */
export function soloDigitos(valor: string, max: number): string {
  return valor.replace(/\D/g, "").slice(0, max)
}
