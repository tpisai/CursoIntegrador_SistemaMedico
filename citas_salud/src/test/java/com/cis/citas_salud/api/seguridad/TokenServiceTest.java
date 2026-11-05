package com.cis.citas_salud.api.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** En producción el backend no debe arrancar con una clave de sesión conocida o corta. */
class TokenServiceTest {

    private static final String CLAVE_DESARROLLO = "saludgrau-clave-de-desarrollo-cambiar-en-produccion";

    private static StandardEnvironment entorno(String... perfiles) {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.setActiveProfiles(perfiles);
        return entorno;
    }

    @ParameterizedTest
    @ValueSource(strings = {CLAVE_DESARROLLO, "corta", "CAMBIAME"})
    void enProduccionRechazaClavesInseguras(String clave) {
        assertThrows(IllegalStateException.class, () -> new TokenService(clave, entorno("prod")));
    }

    @Test
    void enProduccionAceptaUnaClavePropiaLarga() {
        assertDoesNotThrow(() -> new TokenService("0123456789abcdef0123456789abcdef", entorno("prod")));
    }

    @Test
    void enDesarrolloAceptaLaClavePorDefecto() {
        assertDoesNotThrow(() -> new TokenService(CLAVE_DESARROLLO, entorno()));
    }
}
