import { request } from "./http"
import type { LoginRequest, RecuperarPinRequest, RegistroRequest, Sesion } from "./types"

export function iniciarSesion(datos: LoginRequest) {
  return request<Sesion>("POST", "/auth/login", datos)
}

/** Crea una cuenta de paciente o de doctor según datos.tipoCuenta. */
export function registrarCuenta(datos: RegistroRequest) {
  return request<Sesion>("POST", "/auth/registro", datos)
}

export function recuperarPin(datos: RecuperarPinRequest) {
  return request<void>("POST", "/auth/recuperar-pin", datos)
}
