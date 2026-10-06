package com.cis.citas_salud.api.seguridad;

import com.cis.citas_salud.api.ApiException;

/** Usuario autenticado de la petición actual (sale del token). */
public record UsuarioSesion(Integer idUsuario, String rol) {

    public static final String PACIENTE = "PACIENTE";
    public static final String DOCTOR = "DOCTOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public UsuarioSesion exigirRol(String rolEsperado) {
        if (!rolEsperado.equals(rol)) {
            throw ApiException.prohibido("Esta opción no está disponible para tu tipo de cuenta.");
        }
        return this;
    }
}
