package com.cis.citas_salud.api.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Token de sesión firmado con HMAC-SHA256: base64(idUsuario:rol:expiracion).firma.
 * No se guarda en la base de datos; basta la firma para saber que lo emitió este servidor.
 */
@Service
public class TokenService {

    private static final Duration DURACION = Duration.ofHours(12);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();

    private final SecretKeySpec clave;

    public TokenService(@Value("${saludgrau.token.secreto:saludgrau-clave-de-desarrollo-cambiar-en-produccion}") String secreto) {
        this.clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String emitir(Integer idUsuario, String rol) {
        long expira = Instant.now().plus(DURACION).getEpochSecond();
        String datos = B64.encodeToString((idUsuario + ":" + rol + ":" + expira).getBytes(StandardCharsets.UTF_8));
        return datos + "." + firmar(datos);
    }

    public Optional<UsuarioSesion> validar(String token) {
        if (token == null) return Optional.empty();
        int punto = token.indexOf('.');
        if (punto < 1) return Optional.empty();

        String datos = token.substring(0, punto);
        byte[] firma = token.substring(punto + 1).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(firma, firmar(datos).getBytes(StandardCharsets.UTF_8))) return Optional.empty();

        try {
            String[] partes = new String(B64_DECODER.decode(datos), StandardCharsets.UTF_8).split(":");
            if (partes.length != 3 || Long.parseLong(partes[2]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(new UsuarioSesion(Integer.valueOf(partes[0]), partes[1]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(clave);
            return B64.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }
}
