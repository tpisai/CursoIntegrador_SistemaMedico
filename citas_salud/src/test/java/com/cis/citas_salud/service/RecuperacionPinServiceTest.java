package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AuthDto.RecuperarPinRequest;
import com.cis.citas_salud.api.dto.AuthDto.RestablecerPinRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.RecuperacionPin;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.RecuperacionPinRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF-03: el código de 6 dígitos que llega al correo y permite crear un PIN nuevo. */
@ExtendWith(MockitoExtension.class)
class RecuperacionPinServiceTest {

    private static final String CORREO = "ana.quispe@correo.test";
    private static final String CODIGO = "482913";
    private static final String CODIGO_ERRADO = "000000";
    private static final String PIN_ACTUAL = "123456";
    private static final String PIN_NUEVO = "654321";
    private static final String CODIGO_INVALIDO = "El código no es válido o ya venció. Pide uno nuevo.";

    // Costo bajo: el resultado es el mismo y las pruebas no tardan.
    private final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private RecuperacionPinRepository recuperacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CorreoService correoService;
    @Mock
    private NotificacionService notificacionService;

    private RecuperacionPinService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new RecuperacionPinService(recuperacionRepository, usuarioRepository, correoService,
                notificacionService, pinEncoder);
        usuario = DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678");
        usuario.setCorreo(CORREO);
        usuario.setPinHash(pinEncoder.encode(PIN_ACTUAL));
    }

    private void existeElUsuario() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
    }

    private void tieneElCodigo(RecuperacionPin recuperacion) {
        when(recuperacionRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(recuperacion));
    }

    private void noTieneCodigo() {
        when(recuperacionRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.empty());
    }

    /** Código emitido hace 5 minutos y vigente por otros 10. */
    private RecuperacionPin codigoVigente() {
        LocalDateTime ahora = LocalDateTime.now();
        RecuperacionPin recuperacion = new RecuperacionPin();
        recuperacion.setIdRecuperacion(7);
        recuperacion.setUsuario(usuario);
        recuperacion.setCodigoHash(pinEncoder.encode(CODIGO));
        recuperacion.setFechaCreacion(ahora.minusMinutes(5));
        recuperacion.setFechaExpiracion(ahora.plusMinutes(10));
        return recuperacion;
    }

    private static RestablecerPinRequest conCodigo(String codigo) {
        return new RestablecerPinRequest(CORREO, codigo, PIN_NUEVO);
    }

    private void elPinNoCambio() {
        assertTrue(pinEncoder.matches(PIN_ACTUAL, usuario.getPinHash()));
    }

    // ----- Solicitar el código -----

    @Test
    void solicitarGuardaElCodigoCifradoYEnviaElMismoPorCorreo() {
        existeElUsuario();
        noTieneCodigo();

        servicio.solicitar(new RecuperarPinRequest("  " + CORREO + " "));

        ArgumentCaptor<RecuperacionPin> guardada = ArgumentCaptor.forClass(RecuperacionPin.class);
        verify(recuperacionRepository).save(guardada.capture());
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(eq(usuario), anyString(), texto.capture());

        Matcher codigo = Pattern.compile("\\b\\d{6}\\b").matcher(texto.getValue());
        assertTrue(codigo.find(), "El correo debe traer un código de 6 dígitos");
        RecuperacionPin recuperacion = guardada.getValue();
        assertNotEquals(codigo.group(), recuperacion.getCodigoHash());
        assertTrue(pinEncoder.matches(codigo.group(), recuperacion.getCodigoHash()));
        assertSame(usuario, recuperacion.getUsuario());
        assertEquals(0, recuperacion.getIntentos());
        assertEquals(recuperacion.getFechaCreacion().plusMinutes(15), recuperacion.getFechaExpiracion());
    }

    @Test
    void correoInexistenteNoFallaNiEnviaNada() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        servicio.solicitar(new RecuperarPinRequest(CORREO));

        verifyNoInteractions(recuperacionRepository, correoService);
    }

    @Test
    void cuentaDesactivadaNoRecibeCodigo() {
        usuario.setEstado("INACTIVO");
        existeElUsuario();

        servicio.solicitar(new RecuperarPinRequest(CORREO));

        verifyNoInteractions(recuperacionRepository, correoService);
    }

    @Test
    void reenvioAntesDeUnMinutoMantieneElCodigoAnterior() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        recuperacion.setFechaCreacion(LocalDateTime.now().minusSeconds(30));
        String hashAnterior = recuperacion.getCodigoHash();
        tieneElCodigo(recuperacion);

        servicio.solicitar(new RecuperarPinRequest(CORREO));

        assertEquals(hashAnterior, recuperacion.getCodigoHash());
        verify(recuperacionRepository, never()).save(any());
        verifyNoInteractions(correoService);
    }

    @Test
    void reenvioDespuesDeUnMinutoReemplazaElCodigoYReiniciaLosIntentos() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        recuperacion.setFechaCreacion(LocalDateTime.now().minusMinutes(2));
        recuperacion.setIntentos(3);
        LocalDateTime vencimientoAnterior = recuperacion.getFechaExpiracion();
        tieneElCodigo(recuperacion);

        servicio.solicitar(new RecuperarPinRequest(CORREO));

        verify(recuperacionRepository).save(recuperacion);
        verify(correoService).enviar(eq(usuario), anyString(), anyString());
        assertEquals(0, recuperacion.getIntentos());
        assertTrue(recuperacion.getFechaExpiracion().isAfter(vencimientoAnterior));
        assertFalse(pinEncoder.matches(CODIGO, recuperacion.getCodigoHash()), "El código anterior deja de servir");
    }

    // ----- Restablecer el PIN -----

    @Test
    void codigoCorrectoCambiaElPinYBorraElCodigo() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        tieneElCodigo(recuperacion);

        servicio.restablecer(conCodigo(CODIGO));

        assertTrue(pinEncoder.matches(PIN_NUEVO, usuario.getPinHash()));
        assertFalse(pinEncoder.matches(PIN_ACTUAL, usuario.getPinHash()));
        verify(recuperacionRepository).delete(recuperacion);
        verify(notificacionService).crear(eq(usuario), eq(Notificacion.SISTEMA), eq("Cambiaste tu PIN"), anyString());
    }

    @Test
    void codigoCorrectoDesbloqueaLaCuenta() {
        usuario.setIntentosFallidos(2);
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(4));
        existeElUsuario();
        tieneElCodigo(codigoVigente());

        servicio.restablecer(conCodigo(CODIGO));

        assertNull(usuario.getBloqueadoHasta());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void codigoIncorrectoSumaUnIntentoSinCambiarElPin() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        tieneElCodigo(recuperacion);

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO_ERRADO)));

        assertEquals(CODIGO_INVALIDO, error.getMessage());
        assertEquals(1, recuperacion.getIntentos());
        elPinNoCambio();
        verify(recuperacionRepository, never()).delete(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void codigoVencidoSeBorraAunqueSeaElCorrecto() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        recuperacion.setFechaExpiracion(LocalDateTime.now().minusSeconds(1));
        tieneElCodigo(recuperacion);

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO)));

        verify(recuperacionRepository).delete(recuperacion);
        elPinNoCambio();
    }

    @Test
    void codigoYaUsadoNoSirveUnaSegundaVez() {
        existeElUsuario();
        // Al usarlo se borra: la segunda consulta ya no lo encuentra.
        when(recuperacionRepository.findByUsuario_IdUsuario(10))
                .thenReturn(Optional.of(codigoVigente()))
                .thenReturn(Optional.empty());
        servicio.restablecer(conCodigo(CODIGO));
        String hashTrasElCambio = usuario.getPinHash();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.restablecer(new RestablecerPinRequest(CORREO, CODIGO, "111111")));

        assertEquals(CODIGO_INVALIDO, error.getMessage());
        assertEquals(hashTrasElCambio, usuario.getPinHash());
    }

    @Test
    void trasCincoIntentosFallidosNiElCodigoCorrectoSirve() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        tieneElCodigo(recuperacion);

        for (int intento = 1; intento <= 5; intento++) {
            exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO_ERRADO)));
            assertEquals(intento, recuperacion.getIntentos());
        }
        verify(recuperacionRepository, never()).delete(any());

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO)));

        verify(recuperacionRepository).delete(recuperacion);
        elPinNoCambio();
    }

    @Test
    void conCuatroIntentosFallidosElCodigoCorrectoAunSirve() {
        existeElUsuario();
        RecuperacionPin recuperacion = codigoVigente();
        recuperacion.setIntentos(4);
        tieneElCodigo(recuperacion);

        servicio.restablecer(conCodigo(CODIGO));

        assertTrue(pinEncoder.matches(PIN_NUEVO, usuario.getPinHash()));
    }

    @Test
    void correoInexistenteRespondeIgualQueUnCodigoInvalido() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO)));

        assertEquals(CODIGO_INVALIDO, error.getMessage());
        verifyNoInteractions(recuperacionRepository);
    }

    @Test
    void sinCodigoPedidoRespondeIgualQueUnCodigoInvalido() {
        existeElUsuario();
        noTieneCodigo();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.restablecer(conCodigo(CODIGO)));

        assertEquals(CODIGO_INVALIDO, error.getMessage());
        elPinNoCambio();
    }
}
