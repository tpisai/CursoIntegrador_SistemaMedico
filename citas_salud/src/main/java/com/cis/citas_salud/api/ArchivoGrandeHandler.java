package com.cis.citas_salud.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Archivo que supera el límite de ApiConfig. Va aparte de ApiExceptionHandler porque Spring lo
 * detecta antes de saber qué controlador atiende la petición, y ese handler solo cubre el paquete api.
 */
@RestControllerAdvice
public class ArchivoGrandeHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "El archivo supera el máximo de 5 MB."));
    }
}
