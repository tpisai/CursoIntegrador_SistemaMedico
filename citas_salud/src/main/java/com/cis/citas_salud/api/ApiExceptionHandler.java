package com.cis.citas_salud.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** Todas las respuestas de error de la API tienen la forma { "message": "..." }. */
@RestControllerAdvice(basePackages = "com.cis.citas_salud.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return respuesta(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Revisa los datos ingresados.");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> peticionInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud no tiene el formato esperado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {
        log.error("Error no controlado en la API", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error en el servidor. Inténtalo más tarde.");
    }

    private static ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("message", mensaje));
    }
}
