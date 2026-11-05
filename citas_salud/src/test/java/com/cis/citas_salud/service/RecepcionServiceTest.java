package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.ResultadoTicket;
import com.cis.citas_salud.api.dto.AdminDto.TicketResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Auditoria;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.repository.AuditoriaRepository;
import com.cis.citas_salud.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.TICKET;
import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF-12: validación del ticket en recepción, con sus cuatro resultados posibles. */
@ExtendWith(MockitoExtension.class)
class RecepcionServiceTest {

    private static final int ID_CITA = 33;
    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private AdminService adminService;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private NotificacionService notificacionService;

    private RecepcionService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);
    private PersonalAdministrativo admin;
    private Doctor doctor;

    @BeforeEach
    void preparar() {
        servicio = new RecepcionService(adminService, citaRepository, auditoriaRepository, notificacionService);
        admin = new PersonalAdministrativo();
        admin.setUsuario(DatosPrueba.usuario(1, UsuarioSesion.ADMINISTRADOR, "44556677"));
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        // Común a casi todas las pruebas; las de formato del número no lo necesitan.
        lenient().when(adminService.personal(sesion)).thenReturn(admin);
    }

    private Cita existeLaCita(LocalDate fecha, String estado) {
        Paciente paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        Cita cita = DatosPrueba.cita(ID_CITA, paciente, DatosPrueba.horario(5, doctor, fecha), estado);
        when(citaRepository.buscarPorTicket(TICKET)).thenReturn(Optional.of(cita));
        return cita;
    }

    private void sinValidacionPrevia() {
        when(auditoriaRepository.findFirstByAccionAndEntidadAndIdRegistroAndResultadoOrderByFechaDesc(
                Auditoria.VALIDAR_TICKET, Auditoria.ENTIDAD_CITA, ID_CITA, Auditoria.EXITOSO)).thenReturn(Optional.empty());
    }

    private Auditoria validadoAntes() {
        Auditoria previa = new Auditoria(admin.getUsuario(), Auditoria.VALIDAR_TICKET, Auditoria.ENTIDAD_CITA, ID_CITA, "");
        previa.setFecha(LocalDateTime.now().withHour(8).withMinute(45));
        when(auditoriaRepository.findFirstByAccionAndEntidadAndIdRegistroAndResultadoOrderByFechaDesc(
                Auditoria.VALIDAR_TICKET, Auditoria.ENTIDAD_CITA, ID_CITA, Auditoria.EXITOSO)).thenReturn(Optional.of(previa));
        return previa;
    }

    // ----- Validar: registra la llegada solo si el resultado es VALIDO -----

    @Test
    void ticketValidoRegistraLaLlegadaYAvisaAlDoctor() {
        existeLaCita(HOY, Cita.CONFIRMADA);
        sinValidacionPrevia();
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(llamada -> llamada.getArgument(0));

        TicketResponse respuesta = servicio.validar(sesion, TICKET);

        ArgumentCaptor<Auditoria> registro = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(registro.capture());
        assertSame(admin.getUsuario(), registro.getValue().getUsuario());
        assertEquals(Auditoria.VALIDAR_TICKET, registro.getValue().getAccion());
        assertEquals(Auditoria.ENTIDAD_CITA, registro.getValue().getEntidad());
        assertEquals(ID_CITA, registro.getValue().getIdRegistro());
        assertEquals(Auditoria.EXITOSO, registro.getValue().getResultado());

        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.CITA),
                contains("Paciente en recepción"), anyString());
        // Ya quedó registrada: el frontend muestra "Llegada registrada" con este resultado.
        assertEquals(ResultadoTicket.YA_VALIDADO, respuesta.resultado());
        assertNotNull(respuesta.validadoEn());
        assertEquals(Cita.CONFIRMADA, respuesta.estado());
    }

    @Test
    void ticketYaValidadoResponde409ConLaHoraDeLlegada() {
        existeLaCita(HOY, Cita.CONFIRMADA);
        validadoAntes();

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.validar(sesion, TICKET));

        assertEquals("Este ticket ya fue validado a las 08:45.", error.getMessage());
        verify(auditoriaRepository, never()).save(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void ticketDeOtroDiaResponde409() {
        existeLaCita(HOY.plusDays(2), Cita.CONFIRMADA);
        sinValidacionPrevia();

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.validar(sesion, TICKET));

        assertTrue(error.getMessage().startsWith("No le corresponde el turno"));
        verify(auditoriaRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "CANCELADA, La cita fue cancelada. El paciente debe reservar una nueva.",
            "ATENDIDA, Esta cita ya fue atendida.",
            "NO_ASISTIO, El paciente no asistió a esta cita."
    })
    void citaNoActivaResponde409ConElMotivo(String estado, String mensaje) {
        existeLaCita(HOY, estado);
        sinValidacionPrevia();

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.validar(sesion, TICKET));

        assertEquals(mensaje, error.getMessage());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void citaNoActivaPesaMasQueLaFechaYLaValidacionPrevia() {
        existeLaCita(HOY.plusDays(2), Cita.CANCELADA);
        validadoAntes();

        assertEquals(ResultadoTicket.NO_ACTIVA, servicio.consultar(sesion, TICKET).resultado());
    }

    @Test
    void ticketInexistenteResponde404ConElNumeroNormalizado() {
        when(citaRepository.buscarPorTicket("A-0999")).thenReturn(Optional.empty());

        ApiException error = exigeError(HttpStatus.NOT_FOUND, () -> servicio.validar(sesion, "a-999"));

        assertEquals("No existe ninguna cita con el ticket A-0999.", error.getMessage());
    }

    @Test
    void sinPermisoDeAdministracionNoSeConsultaLaCita() {
        when(adminService.personal(sesion)).thenThrow(ApiException.prohibido("Sin permiso."));

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.validar(sesion, TICKET));

        verifyNoInteractions(citaRepository, auditoriaRepository);
    }

    // ----- Consultar: informa el resultado sin registrar nada -----

    @Test
    void consultarTicketValidoNoRegistraLaLlegada() {
        existeLaCita(HOY, Cita.RESERVADA);
        sinValidacionPrevia();

        TicketResponse respuesta = servicio.consultar(sesion, TICKET);

        assertEquals(ResultadoTicket.VALIDO, respuesta.resultado());
        assertTrue(respuesta.mensaje().startsWith("Ticket válido"));
        assertNull(respuesta.validadoEn());
        verify(auditoriaRepository, never()).save(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void consultarTicketYaValidadoMuestraCuando() {
        existeLaCita(HOY, Cita.CONFIRMADA);
        Auditoria previa = validadoAntes();

        TicketResponse respuesta = servicio.consultar(sesion, TICKET);

        assertEquals(ResultadoTicket.YA_VALIDADO, respuesta.resultado());
        assertEquals(Formato.fechaHora(previa.getFecha()), respuesta.validadoEn());
    }

    @Test
    void consultarTicketDeOtroDiaLoInforma() {
        existeLaCita(HOY.minusDays(1), Cita.CONFIRMADA);
        sinValidacionPrevia();

        assertEquals(ResultadoTicket.OTRO_DIA, servicio.consultar(sesion, TICKET).resultado());
    }

    // ----- Casos que antes fallaban -----

    @Test
    void validacionDeUnDiaAnteriorNoDeberiaContarSiLaCitaSeReprogramoParaHoy() {
        existeLaCita(HOY, Cita.CONFIRMADA);
        validadoAntes().setFecha(LocalDateTime.now().minusDays(3));

        assertEquals(ResultadoTicket.VALIDO, servicio.consultar(sesion, TICKET).resultado());
    }

    // ----- Formato del número -----

    @ParameterizedTest
    @ValueSource(strings = {"A-0154", "a-0154", "A0154", "a154", "154", "0154", "  a-154  "})
    void lasFormasDeEscribirElTicketLlevanAlMismoNumero(String escrito) {
        assertEquals(TICKET, RecepcionService.normalizar(escrito));
    }

    @Test
    void textoQueNoEsUnTicketSeConservaEnMayusculas() {
        assertEquals("XYZ", RecepcionService.normalizar(" xyz "));
        assertEquals("", RecepcionService.normalizar(null));
    }
}
