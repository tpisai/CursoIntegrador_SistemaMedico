package com.cis.citas_salud.service;

import com.cis.citas_salud.api.dto.SolicitudDto.CambioPacienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.CambioPendienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.DecisionPaciente;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolverCambioRequest;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolucionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** RF-17: el paciente resuelve un cambio de fecha aprobado y descarga la constancia. */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    private static final LocalDate EN_TRES_DIAS = LocalDate.now().plusDays(3);
    private static final LocalDate AYER = LocalDate.now().minusDays(1);
    private static final String FOTO_ORIGINAL =
            "{\"fechaOriginal\":\"" + EN_TRES_DIAS + "\",\"horaOriginal\":\"09:00\","
                    + "\"consultorioOriginal\":\"Módulo A\",\"doctorOriginal\":\"Ana Quispe R.\","
                    + "\"especialidadOriginal\":\"Medicina General\"}";

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private CitaService citaService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private GeneradorReportesSql generador;

    private SolicitudService servicio;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UsuarioSesion sesion = new UsuarioSesion(10, UsuarioSesion.PACIENTE);
    private Paciente paciente;
    private Doctor doctor;
    private Horario original;
    private Cita cita;

    @BeforeEach
    void preparar() {
        servicio = new SolicitudService(solicitudRepository, citaRepository, horarioRepository,
                pacienteRepository, citaService, notificacionService, generador, objectMapper);
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        original = DatosPrueba.horario(5, doctor, EN_TRES_DIAS);
        original.setEstado(Horario.RESERVADO);
        cita = DatosPrueba.cita(33, paciente, original, Cita.CONFIRMADA);
    }

    private void existeElPaciente() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
    }

    private Solicitud aprobada(String datos) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(7);
        solicitud.setCita(cita);
        solicitud.setDoctor(doctor);
        solicitud.setTipo(Solicitud.CAMBIO_HORARIO);
        solicitud.setMotivo("Reunión clínica");
        solicitud.setEstado(Solicitud.APROBADA);
        solicitud.setFechaSolicitud(LocalDateTime.now().minusDays(1));
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitud.setRespuestaAdmin("Aprobado: el paciente elige el nuevo horario.");
        solicitud.setDatosAdicionales(datos);
        return solicitud;
    }

    /** Lo que devuelve la base al bloquear la solicitud y cargar su cita. */
    private Solicitud abrir(Solicitud solicitud) {
        when(solicitudRepository.bloquear(7)).thenReturn(Optional.of(solicitud));
        when(citaRepository.buscarConDetalle(33)).thenReturn(Optional.of(cita));
        return solicitud;
    }

    // ----- Pendientes del portal -----

    @Test
    void pendientesMuestraLaCitaOriginalAprobada() {
        existeElPaciente();
        when(solicitudRepository.cambiosAprobadosDelPaciente(1)).thenReturn(List.of(aprobada(FOTO_ORIGINAL)));

        List<CambioPendienteResponse> pendientes = servicio.pendientes(sesion);

        assertEquals(1, pendientes.size());
        CambioPendienteResponse pendiente = pendientes.get(0);
        assertEquals(7, pendiente.idSolicitud());
        assertEquals(33, pendiente.idCita());
        assertEquals(3, pendiente.idDoctor());
        assertEquals(1, pendiente.idEspecialidad());
        assertEquals(1, pendiente.idConsultorio());
        assertEquals(EN_TRES_DIAS.toString(), pendiente.fecha());
        assertEquals("09:00", pendiente.hora());
        assertEquals("Módulo A", pendiente.consultorio());
        assertEquals("Reunión clínica", pendiente.motivo());
    }

    @Test
    void pendientesIgnoraLasCitasQueYaPasaron() {
        existeElPaciente();
        Horario viejo = DatosPrueba.horario(6, doctor, AYER);
        viejo.setEstado(Horario.RESERVADO);
        Cita citaVieja = DatosPrueba.cita(34, paciente, viejo, Cita.CONFIRMADA);
        Solicitud pasada = aprobada(FOTO_ORIGINAL);
        pasada.setCita(citaVieja);
        when(solicitudRepository.cambiosAprobadosDelPaciente(1)).thenReturn(List.of(pasada));

        assertTrue(servicio.pendientes(sesion).isEmpty());
    }

    @Test
    void miasIndicaCuandoLaConstanciaEstaDisponible() {
        existeElPaciente();
        Solicitud aprobada = aprobada(FOTO_ORIGINAL);
        Solicitud pendiente = aprobada(FOTO_ORIGINAL);
        pendiente.setIdSolicitud(8);
        pendiente.setEstado(Solicitud.PENDIENTE);
        when(solicitudRepository.cambiosDelPaciente(1)).thenReturn(List.of(aprobada, pendiente));

        List<CambioPacienteResponse> filas = servicio.mias(sesion);

        assertTrue(filas.get(0).constanciaDisponible());
        assertFalse(filas.get(1).constanciaDisponible());
    }

    // ----- Decisiones del paciente -----

    @Test
    void resolverConElMismoDoctorReprogramaLaCitaYLiberaElTurnoAnterior() {
        existeElPaciente();
        Solicitud solicitud = abrir(aprobada(FOTO_ORIGINAL));
        Horario nuevo = DatosPrueba.horario(21, doctor, EN_TRES_DIAS.plusDays(1));
        when(horarioRepository.bloquearVarios(any())).thenReturn(List.of(original, nuevo));

        ResolucionResponse respuesta = servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.MISMO_DOCTOR, 21));

        assertEquals(Solicitud.RESUELTA, solicitud.getEstado());
        assertEquals(Solicitud.RESUELTA, respuesta.estado());
        assertEquals(Solicitud.MISMO_DOCTOR, respuesta.decision());
        assertEquals(Horario.DISPONIBLE, original.getEstado());
        assertEquals(Horario.RESERVADO, nuevo.getEstado());
        assertSame(nuevo, cita.getHorario());
        assertTrue(respuesta.mensaje().contains("reprogramada"));
        assertTrue(solicitud.getDatosAdicionales().contains("\"decisionPaciente\":\"MISMO_DOCTOR\""));
        assertTrue(solicitud.getDatosAdicionales().contains("\"nuevaFecha\":\"" + EN_TRES_DIAS.plusDays(1) + "\""));
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("El paciente cambió su cita"), contains("movió su cita"));
    }

    @Test
    void resolverConOtroDoctorCancelaLaCitaYReservaUnaNueva() {
        existeElPaciente();
        Solicitud solicitud = abrir(aprobada(FOTO_ORIGINAL));
        Doctor otro = DatosPrueba.doctor(4, DatosPrueba.usuario(21, UsuarioSesion.DOCTOR, "11223344"));
        Horario nuevo = DatosPrueba.horario(22, otro, EN_TRES_DIAS.plusDays(2));
        when(horarioRepository.bloquearVarios(any())).thenReturn(List.of(original, nuevo));
        when(citaRepository.siguienteTicket()).thenReturn(155L);
        when(citaRepository.save(any(Cita.class))).thenAnswer(llamada -> {
            Cita nueva = llamada.getArgument(0);
            nueva.setIdCita(44);
            return nueva;
        });

        ResolucionResponse respuesta = servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.OTRO_DOCTOR, 22));

        verify(citaService).cancelarCita(cita);
        assertEquals(Solicitud.RESUELTA, solicitud.getEstado());
        assertEquals(Solicitud.OTRO_DOCTOR, respuesta.decision());
        assertEquals(44, respuesta.idCitaNueva());
        assertEquals("A-0155", respuesta.numeroTicket());
        assertTrue(respuesta.mensaje().contains("nuevo ticket"));
        assertTrue(solicitud.getDatosAdicionales().contains("\"nuevoTicket\":\"A-0155\""));
        verify(notificacionService).crear(eq(otro.getUsuario()), eq(Notificacion.CITA),
                eq("Nueva cita asignada"), anyString());
    }

    @Test
    void resolverCancelandoDejaLaCitaCancelada() {
        existeElPaciente();
        Solicitud solicitud = abrir(aprobada(FOTO_ORIGINAL));

        ResolucionResponse respuesta = servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.CANCELADA, null));

        verify(citaService).cancelarCita(cita);
        assertEquals(Solicitud.RESUELTA, solicitud.getEstado());
        assertEquals(Solicitud.CANCELADA, respuesta.decision());
        assertNull(respuesta.idCitaNueva());
        assertTrue(respuesta.mensaje().contains("Cancelamos"));
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Cita cancelada por el paciente"), contains("canceló su cita"));
    }

    @Test
    void resolverConUnDoctorDeOtraEspecialidadResponde400() {
        existeElPaciente();
        abrir(aprobada(FOTO_ORIGINAL));
        Doctor cardiologo = DatosPrueba.doctor(4, DatosPrueba.usuario(21, UsuarioSesion.DOCTOR, "11223344"));
        Especialidad cardiologia = new Especialidad("Cardiología");
        cardiologia.setIdEspecialidad(2);
        cardiologo.setEspecialidad(cardiologia);
        Horario nuevo = DatosPrueba.horario(22, cardiologo, EN_TRES_DIAS.plusDays(2));
        when(horarioRepository.bloquearVarios(any())).thenReturn(List.of(original, nuevo));

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.OTRO_DOCTOR, 22)));

        verify(citaService, never()).cancelarCita(any());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void resolverSinHorarioElegidoResponde400() {
        existeElPaciente();
        abrir(aprobada(FOTO_ORIGINAL));

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.MISMO_DOCTOR, null)));

        verifyNoInteractions(horarioRepository);
    }

    @Test
    void resolverUnaSolicitudYaResueltaResponde409() {
        existeElPaciente();
        Solicitud solicitud = abrir(aprobada(FOTO_ORIGINAL));
        solicitud.setEstado(Solicitud.RESUELTA);

        exigeError(HttpStatus.CONFLICT, () -> servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.CANCELADA, null)));

        verify(citaService, never()).cancelarCita(any());
    }

    @Test
    void resolverLaSolicitudDeOtroPacienteResponde404() {
        Solicitud solicitud = abrir(aprobada(FOTO_ORIGINAL));
        Paciente otro = DatosPrueba.paciente(2, DatosPrueba.usuario(11, UsuarioSesion.PACIENTE, "99999999"));
        cita.setPaciente(otro);
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.resolver(sesion, 7,
                new ResolverCambioRequest(DecisionPaciente.CANCELADA, null)));
    }

    // ----- Constancia -----

    @Test
    void laConstanciaSeGeneraConLaPlantillaDeJasper() {
        Solicitud solicitud = aprobada(FOTO_ORIGINAL);
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));
        byte[] pdf = {1, 2, 3};
        when(generador.pdf("ConstanciaCambioFecha", Map.of("ID_SOLICITUD", 7))).thenReturn(pdf);

        Archivo archivo = servicio.constancia(sesion, 7);

        assertEquals("constancia-cambio-fecha-7.pdf", archivo.nombre());
        assertEquals("application/pdf", archivo.tipoMime());
        assertArrayEquals(pdf, archivo.contenido());
    }

    @Test
    void laConstanciaDeOtraSolicitudNoEsVisibleParaElPaciente() {
        Solicitud solicitud = aprobada(FOTO_ORIGINAL);
        solicitud.setCita(DatosPrueba.cita(33, DatosPrueba.paciente(2,
                DatosPrueba.usuario(11, UsuarioSesion.PACIENTE, "99999999")), original, Cita.CONFIRMADA));
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.constancia(sesion, 7));

        verifyNoInteractions(generador);
    }

    @Test
    void laConstanciaSoloExisteCuandoAdministracionAprobo() {
        Solicitud solicitud = aprobada(FOTO_ORIGINAL);
        solicitud.setEstado(Solicitud.PENDIENTE);
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));

        exigeError(HttpStatus.CONFLICT, () -> servicio.constancia(sesion, 7));

        verifyNoInteractions(generador);
    }
}
