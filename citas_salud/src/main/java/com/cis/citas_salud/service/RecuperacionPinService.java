package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AuthDto.RecuperarPinRequest;
import com.cis.citas_salud.api.dto.AuthDto.RestablecerPinRequest;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.RecuperacionPin;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.RecuperacionPinRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/** RF-03: recupera el PIN con un código de 6 dígitos enviado al correo registrado. */
@Service
@Transactional
public class RecuperacionPinService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPinService.class);
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final Duration ESPERA_REENVIO = Duration.ofMinutes(1);
    private static final int MAX_INTENTOS = 5;

    private final RecuperacionPinRepository recuperacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoService correoService;
    private final NotificacionService notificacionService;
    private final BCryptPasswordEncoder pinEncoder;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionPinService(RecuperacionPinRepository recuperacionRepository, UsuarioRepository usuarioRepository,
                                  CorreoService correoService, NotificacionService notificacionService,
                                  BCryptPasswordEncoder pinEncoder) {
        this.recuperacionRepository = recuperacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoService = correoService;
        this.notificacionService = notificacionService;
        this.pinEncoder = pinEncoder;
    }

    /** Se responde igual exista o no el correo, para no revelar qué correos están registrados. */
    public void solicitar(RecuperarPinRequest datos) {
        usuarioRepository.findByCorreoIgnoreCase(datos.correo().trim())
                .filter(usuario -> "ACTIVO".equals(usuario.getEstado()))
                .ifPresent(this::enviarCodigo);
    }

    private void enviarCodigo(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        RecuperacionPin recuperacion = recuperacionRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseGet(RecuperacionPin::new);
        if (recuperacion.getIdRecuperacion() != null
                && recuperacion.getFechaCreacion().plus(ESPERA_REENVIO).isAfter(ahora)) {
            return;
        }

        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        recuperacion.setUsuario(usuario);
        recuperacion.setCodigoHash(pinEncoder.encode(codigo));
        recuperacion.setFechaCreacion(ahora);
        recuperacion.setFechaExpiracion(ahora.plus(VIGENCIA));
        recuperacion.setIntentos(0);
        recuperacionRepository.save(recuperacion);

        correoService.enviar(usuario, "Tu código para recuperar el PIN",
                "Tu código para crear un nuevo PIN es " + codigo + ". Vence en " + VIGENCIA.toMinutes() + " minutos.\n\n"
                        + "Si no lo pediste, ignora este mensaje: tu PIN sigue siendo el mismo.");
        // Sin SMTP (en desarrollo) el correo no sale: el código queda en el log para poder probar el flujo.
        if (!correoService.configurado()) {
            log.info("Código de recuperación de PIN para {}: {}", usuario.getCorreo(), codigo);
        }
    }

    // noRollbackFor: los intentos fallidos deben guardarse aunque se responda con error.
    @Transactional(noRollbackFor = ApiException.class)
    public void restablecer(RestablecerPinRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(datos.correo().trim())
                .orElseThrow(RecuperacionPinService::codigoInvalido);
        RecuperacionPin recuperacion = recuperacionRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(RecuperacionPinService::codigoInvalido);

        if (recuperacion.getFechaExpiracion().isBefore(LocalDateTime.now()) || recuperacion.getIntentos() >= MAX_INTENTOS) {
            recuperacionRepository.delete(recuperacion);
            throw codigoInvalido();
        }
        if (!pinEncoder.matches(datos.codigo(), recuperacion.getCodigoHash())) {
            recuperacion.setIntentos(recuperacion.getIntentos() + 1);
            throw codigoInvalido();
        }

        usuario.setPinHash(pinEncoder.encode(datos.pinNuevo()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        recuperacionRepository.delete(recuperacion);
        notificacionService.crear(usuario, Notificacion.SISTEMA, "Cambiaste tu PIN",
                "Tu PIN se actualizó con un código enviado a tu correo. Si no fuiste tú, acércate a admisión.");
    }

    private static ApiException codigoInvalido() {
        return ApiException.datosInvalidos("El código no es válido o ya venció. Pide uno nuevo.");
    }
}
