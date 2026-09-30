package com.cis.citas_salud.api;

import org.springframework.http.HttpStatus;

/** Error con un mensaje listo para mostrar al usuario en el frontend. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    public static ApiException datosInvalidos(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException noAutorizado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }
}
