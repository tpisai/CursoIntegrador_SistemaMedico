package com.cis.citas_salud.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {
    }

    public record LoginRequest(
            @NotBlank(message = "Ingresa tu DNI.") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
            @NotBlank(message = "Ingresa tu PIN.") @Pattern(regexp = "\\d{6}", message = "El PIN debe tener 6 dígitos numéricos.") String pin) {
    }

    public enum TipoCuenta { PACIENTE, DOCTOR }

    public record RegistroRequest(
            @NotNull(message = "Elige el tipo de cuenta.") TipoCuenta tipoCuenta,
            @NotBlank(message = "Ingresa tus nombres.") @Size(max = 100, message = "Tus nombres son demasiado largos.") String nombres,
            @NotBlank(message = "Ingresa tus apellidos.") @Size(max = 100, message = "Tus apellidos son demasiado largos.") String apellidos,
            @NotBlank(message = "Ingresa tu DNI.") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
            @NotBlank(message = "Ingresa tu correo.") @Email(message = "Ingresa un correo electrónico válido.") @Size(max = 150) String correo,
            @NotBlank(message = "Ingresa un PIN.") @Pattern(regexp = "\\d{6}", message = "El PIN debe tener 6 dígitos numéricos.") String pin,
            @AssertTrue(message = "Debes aceptar los términos y condiciones.") boolean terminosAceptados,
            // Solo para cuentas de doctor.
            Integer idEspecialidad,
            @Pattern(regexp = "\\d{5,6}", message = "El CMP debe tener 5 o 6 dígitos.") String cmp) {
    }

    public record RecuperarPinRequest(
            @NotBlank(message = "Ingresa tu correo.") @Email(message = "Ingresa un correo electrónico válido.") String correo) {
    }

    public record UsuarioResponse(
            Integer idUsuario,
            String dni,
            String nombres,
            String apellidos,
            String correo,
            String rol,
            // Nombre de la especialidad si el usuario es doctor.
            String especialidad) {
    }

    public record SesionResponse(String token, UsuarioResponse usuario) {
    }
}
