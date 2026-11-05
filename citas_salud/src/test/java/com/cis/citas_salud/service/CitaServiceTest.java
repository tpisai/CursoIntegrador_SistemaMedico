package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.CitaResponse;
import com.cis.citas_salud.api.dto.CitaDto.ReservarCitaRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF-06 a RF-08: reserva de turnos, cancelación con liberación del cupo y citas ajenas. */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final LocalDate EN_TRES_DIAS = LocalDate.now().plusDays(3);
    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private DocumentoMedicoRepository documentoRepository;
    @Mock
    private NotificacionService notificacionService;

    private CitaService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(10, UsuarioSesion.PACIENTE);
    private Paciente paciente;
    private Doctor doctor;

    @BeforeEach
    void preparar() {
        servicio = new CitaService(citaRepository, horarioRepository, pacienteRepository, documentoRepository,
                notificacionService, "archivos");
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
    }

    private void existeElPaciente() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
    }

    /** Lo que hace la base al guardar la cita: asignarle su id y devolverla con el detalle. */
    private void citaSeGuardaConId(int idCita) {
        when(citaRepository.save(any(Cita.class))).thenAnswer(llamada -> {
            Cita nueva = llamada.getArgument(0);
            nueva.setIdCita(idCita);
            when(citaRepository.buscarConDetalle(idCita)).thenReturn(Optional.of(nueva));
            return nueva;
        });
    }

    private Cita citaDe(Paciente dueno, LocalDate fecha, String estado) {
        Horario horario = DatosPrueba.horario(5, doctor, fecha);
        horario.setEstado(Horario.RESERVADO);
        Cita cita = DatosPrueba.cita(33, dueno, horario, estado);
        when(citaRepository.buscarConDetalle(33)).thenReturn(Optional.of(cita));
        return cita;
    }

    // ----- Reserva -----

    @Test
    void reservarTurnoLibreConfirmaLaCitaYOcupaElHorario() {
        existeElPaciente();
        Horario horario = DatosPrueba.horario(5, doctor, EN_TRES_DIAS);
        when(horarioRepository.bloquear(5)).thenReturn(Optional.of(horario));
        when(citaRepository.siguienteTicket()).thenReturn(171L);
        citaSeGuardaConId(33);

        CitaResponse respuesta = servicio.reservar(sesion, new ReservarCitaRequest(5, "  Control general  "));

        assertEquals(Horario.RESERVADO, horario.getEstado());
        assertEquals(33, respuesta.idCita());
        assertEquals("A-0171", respuesta.numeroTicket());
        assertEquals(Cita.RESERVADA, respuesta.estado());
        assertEquals(EN_TRES_DIAS.toString(), respuesta.fecha());
        assertEquals("09:00", respuesta.horaInicio());
        assertEquals("Medicina General", respuesta.especialidad());

        ArgumentCaptor<Cita> guardada = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(guardada.capture());
        assertSame(paciente, guardada.getValue().getPaciente());
        assertSame(horario, guardada.getValue().getHorario());
        assertEquals("Control general", guardada.getValue().getMotivo());

        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.CITA), contains("A-0171"), anyString());
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.CITA), eq("Nueva cita asignada"), anyString());
    }

    @Test
    void reservarConMotivoEnBlancoLoGuardaVacio() {
        existeElPaciente();
        when(horarioRepository.bloquear(5)).thenReturn(Optional.of(DatosPrueba.horario(5, doctor, EN_TRES_DIAS)));
        when(citaRepository.siguienteTicket()).thenReturn(1L);
        citaSeGuardaConId(33);

        servicio.reservar(sesion, new ReservarCitaRequest(5, "   "));

        ArgumentCaptor<Cita> guardada = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(guardada.capture());
        assertNull(guardada.getValue().getMotivo());
    }

    @Test
    void reservarTurnoYaTomadoResponde409SinCrearCita() {
        existeElPaciente();
        Horario horario = DatosPrueba.horario(5, doctor, EN_TRES_DIAS);
        horario.setEstado(Horario.RESERVADO);
        when(horarioRepository.bloquear(5)).thenReturn(Optional.of(horario));

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.reservar(sesion, new ReservarCitaRequest(5, null)));

        assertTrue(error.getMessage().contains("otro paciente"));
        verify(citaRepository, never()).save(any());
        verify(citaRepository, never()).siguienteTicket();
        verifyNoInteractions(notificacionService);
    }

    @Test
    void reservarTurnoQueYaPasoResponde409() {
        existeElPaciente();
        when(horarioRepository.bloquear(5)).thenReturn(Optional.of(DatosPrueba.horario(5, doctor, AYER)));

        exigeError(HttpStatus.CONFLICT, () -> servicio.reservar(sesion, new ReservarCitaRequest(5, null)));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void reservarHorarioInexistenteResponde404() {
        existeElPaciente();
        when(horarioRepository.bloquear(5)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.reservar(sesion, new ReservarCitaRequest(5, null)));
    }

    @Test
    void unDoctorNoPuedeReservarResponde403() {
        UsuarioSesion sesionDoctor = new UsuarioSesion(20, UsuarioSesion.DOCTOR);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.reservar(sesionDoctor, new ReservarCitaRequest(5, null)));

        verifyNoInteractions(horarioRepository, citaRepository);
    }

    @Test
    void cuentaSinPerfilDePacienteResponde403() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.empty());

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.reservar(sesion, new ReservarCitaRequest(5, null)));

        verifyNoInteractions(horarioRepository);
    }

    // ----- Cancelación -----

    @Test
    void cancelarCitaFuturaAbreUnTurnoNuevoEnElMismoHorario() {
        existeElPaciente();
        Cita cita = citaDe(paciente, EN_TRES_DIAS, Cita.CONFIRMADA);
        Horario original = cita.getHorario();

        CitaResponse respuesta = servicio.cancelar(sesion, 33);

        assertEquals(Cita.CANCELADA, respuesta.estado());
        assertEquals(Cita.CANCELADA, cita.getEstado());
        // cita.id_horario es UNIQUE: el horario original se queda con la cita cancelada.
        assertEquals(Horario.CANCELADO, original.getEstado());

        ArgumentCaptor<Horario> reabierto = ArgumentCaptor.forClass(Horario.class);
        verify(horarioRepository).save(reabierto.capture());
        Horario nuevo = reabierto.getValue();
        assertNull(nuevo.getIdHorario());
        assertEquals(Horario.DISPONIBLE, nuevo.getEstado());
        assertSame(doctor, nuevo.getDoctor());
        assertSame(original.getConsultorio(), nuevo.getConsultorio());
        assertEquals(original.getFecha(), nuevo.getFecha());
        assertEquals(original.getHoraInicio(), nuevo.getHoraInicio());
        assertEquals(original.getHoraFin(), nuevo.getHoraFin());

        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.CITA),
                eq("Cita cancelada por el paciente"), anyString());
    }

    @Test
    void cancelarCitaDeUnTurnoPasadoNoAbreOtroTurno() {
        existeElPaciente();
        Cita cita = citaDe(paciente, AYER, Cita.RESERVADA);

        servicio.cancelar(sesion, 33);

        assertEquals(Cita.CANCELADA, cita.getEstado());
        verify(horarioRepository, never()).save(any());
    }

    @Test
    void cancelarCitaAjenaResponde404SinTocarla() {
        existeElPaciente();
        Usuario otraCuenta = DatosPrueba.usuario(11, UsuarioSesion.PACIENTE, "45879621");
        Cita cita = citaDe(DatosPrueba.paciente(2, otraCuenta), EN_TRES_DIAS, Cita.CONFIRMADA);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.cancelar(sesion, 33));

        assertEquals(Cita.CONFIRMADA, cita.getEstado());
        assertEquals(Horario.RESERVADO, cita.getHorario().getEstado());
        verify(horarioRepository, never()).save(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void cancelarCitaInexistenteResponde404() {
        existeElPaciente();
        when(citaRepository.buscarConDetalle(33)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.cancelar(sesion, 33));
    }

    @ParameterizedTest
    @ValueSource(strings = {Cita.CANCELADA, Cita.ATENDIDA, Cita.NO_ASISTIO})
    void cancelarCitaQueYaNoEstaActivaResponde409(String estado) {
        existeElPaciente();
        Cita cita = citaDe(paciente, EN_TRES_DIAS, estado);

        exigeError(HttpStatus.CONFLICT, () -> servicio.cancelar(sesion, 33));

        assertEquals(estado, cita.getEstado());
        verify(horarioRepository, never()).save(any());
    }

    // ----- Ticket -----

    @Test
    void elPacienteDescargaElTicketDeSuCita() {
        citaDe(paciente, EN_TRES_DIAS, Cita.CONFIRMADA);

        CitaService.Archivo ticket = servicio.ticket(sesion, 33);

        assertEquals("ticket-" + DatosPrueba.TICKET + ".pdf", ticket.nombre());
        assertEquals("application/pdf", ticket.tipoMime());
        assertTrue(ticket.contenido().length > 0);
    }

    @Test
    void ticketDeCitaAjenaResponde404() {
        Usuario otraCuenta = DatosPrueba.usuario(11, UsuarioSesion.PACIENTE, "45879621");
        citaDe(DatosPrueba.paciente(2, otraCuenta), EN_TRES_DIAS, Cita.CONFIRMADA);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.ticket(sesion, 33));
    }

    // ----- Confirmar asistencia (EDT 4.12) -----

    @Test
    void confirmarPasaLaCitaReservadaAConfirmada() {
        existeElPaciente();
        Cita cita = citaDe(paciente, EN_TRES_DIAS, Cita.RESERVADA);

        assertEquals(Cita.CONFIRMADA, servicio.confirmar(sesion, 33).estado());
        assertEquals(Cita.CONFIRMADA, cita.getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {Cita.CONFIRMADA, Cita.ATENDIDA, Cita.CANCELADA, Cita.NO_ASISTIO})
    void soloSeConfirmaUnaCitaReservada(String estado) {
        existeElPaciente();
        Cita cita = citaDe(paciente, EN_TRES_DIAS, estado);

        exigeError(HttpStatus.CONFLICT, () -> servicio.confirmar(sesion, 33));

        assertEquals(estado, cita.getEstado());
    }

    @Test
    void noSeConfirmaUnaCitaDeUnTurnoQueYaPaso() {
        existeElPaciente();
        Cita cita = citaDe(paciente, AYER, Cita.RESERVADA);

        exigeError(HttpStatus.CONFLICT, () -> servicio.confirmar(sesion, 33));

        assertEquals(Cita.RESERVADA, cita.getEstado());
    }

    @Test
    void confirmarCitaAjenaResponde404SinTocarla() {
        existeElPaciente();
        Usuario otraCuenta = DatosPrueba.usuario(99, UsuarioSesion.PACIENTE, "99999999");
        Cita cita = citaDe(DatosPrueba.paciente(2, otraCuenta), EN_TRES_DIAS, Cita.RESERVADA);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.confirmar(sesion, 33));

        assertEquals(Cita.RESERVADA, cita.getEstado());
    }
}
