package com.cis.citas_salud.api;

import com.cis.citas_salud.api.dto.AuthDto.LoginRequest;
import com.cis.citas_salud.api.dto.AuthDto.RecuperarPinRequest;
import com.cis.citas_salud.api.dto.AuthDto.RegistroRequest;
import com.cis.citas_salud.api.dto.AuthDto.SesionResponse;
import com.cis.citas_salud.api.dto.AuthDto.UsuarioResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-01 a RF-04: registro, login con DNI + PIN y recuperación de PIN. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest datos) {
        return authService.login(datos);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registro(@Valid @RequestBody RegistroRequest datos) {
        return authService.registrar(datos);
    }

    @PostMapping("/recuperar-pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarPin(@Valid @RequestBody RecuperarPinRequest datos) {
        authService.recuperarPin(datos);
    }

    @GetMapping("/yo")
    public UsuarioResponse yo(UsuarioSesion sesion) {
        return authService.usuarioActual(sesion);
    }
}
