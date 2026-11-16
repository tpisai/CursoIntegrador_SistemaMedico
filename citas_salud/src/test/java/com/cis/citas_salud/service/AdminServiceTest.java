package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosResponse;
import com.cis.citas_salud.api.dto.AdminDto.Decision;
import com.cis.citas_salud.api.dto.AdminDto.ResponderSolicitudRequest;
import com.cis.citas_salud.api.dto.AdminDto.SolicitudAdminResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.CampaniaRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PersonalAdministrativoRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF-13, RF-17 y RF-18: respuesta a las solicitudes de los doctores y creación de horarios. */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final int ID_SOLICITUD = 7;
    private static final int ID_HORARIO_ANTERIOR = 5;
    private static final int ID_HORARIO_NUEVO = 21;
    private static final int ID_DOCTOR = 3;
    private static final int ID_CONSULTORIO = 1;
    private static final int ID_PERSONAL = 2;
    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate EN_CINCO_DIAS = HOY.plusDays(5);
    private static final LocalDate EN_DIEZ_DIAS = HOY.plusDays(10);
    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DOCE = LocalTime.of(12, 0);

    @Mock
    private PersonalAdministrativoRepository personalRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private CampaniaRepository campaniaRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private HorarioGeneradorService horarioGenerador;
    @Mock
    private NotificacionService notificacionService;

    private AdminService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);
    private PersonalAdministrativo personal;
    private Doctor doctor;
    private Paciente paciente;
    private Horario anterior;
    private Horario nuevo;
    private Cita cita;

    @BeforeEach
    void preparar() {
        servicio = new AdminService(personalRepository, citaRepository, solicitudRepository, campaniaRepository,
                doctorRepository, horarioRepository, horarioGenerador, notificacionService, JsonMapper.builder().build());
        personal = new PersonalAdministrativo();
        personal.setIdPersonalAdministrativo(ID_PERSONAL);
        personal.setUsuario(DatosPrueba.usuario(1, UsuarioSesion.ADMINISTRADOR, "44556677"));
        doctor = DatosPrueba.doctor(ID_DOCTOR, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        doctor.getConsultorios().add(DatosPrueba.consultorio(ID_CONSULTORIO));
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        anterior = DatosPrueba.horario(ID_HORARIO_ANTERIOR, doctor, EN_CINCO_DIAS);
        anterior.setEstado(Horario.RESERVADO);
        nuevo = DatosPrueba.horario(ID_HORARIO_NUEVO, doctor, EN_DIEZ_DIAS);
        cita = DatosPrueba.cita(33, paciente, anterior, Cita.CONFIRMADA);
    }

    private void existeElPersonal() {
        when(personalRepository.findByUsuario_IdUsuario(1)).thenReturn(Optional.of(personal));
    }

    private Solicitud solicitud(String tipo, String datosAdicionales) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(ID_SOLICITUD);
        solicitud.setTipo(tipo);
        solicitud.setCita(cita);
        solicitud.setDoctor(doctor);
        solicitud.setMotivo("Motivo del doctor");
        solicitud.setDatosAdicionales(datosAdicionales);
        return solicitud;
    }

    private Solicitud cambioDeFecha() {
        return solicitud(Solicitud.CAMBIO_HORARIO, """
                {"idHorarioNuevo":%d,"fecha":"%s","horaInicio":"09:00","consultorio":"Módulo A · Consultorio 1"}
                """.formatted(ID_HORARIO_NUEVO, EN_DIEZ_DIAS));
    }

    private Solicitud derivacion() {
        return solicitud(Solicitud.DERIVACION, """
                {"establecimiento":"Hospital de Chosica","especialidad":"Cardiología"}
                """);
    }

    /** Deja lista la solicitud para responderla: personal activo, solicitud y cita encontradas. */
    private Solicitud pendiente(Solicitud solicitud) {
        existeElPersonal();
        when(solicitudRepository.bloquear(ID_SOLICITUD)).thenReturn(Optional.of(solicitud));
        when(citaRepository.buscarConDetalle(33)).thenReturn(Optional.of(cita));
        return solicitud;
    }

    private void horariosBloqueados(Horario... horarios) {
        when(horarioRepository.bloquearVarios(List.of(ID_HORARIO_ANTERIOR, ID_HORARIO_NUEVO))).thenReturn(List.of(horarios));
    }

    private SolicitudAdminResponse aprobar() {
        return servicio.responder(sesion, ID_SOLICITUD, new ResponderSolicitudRequest(Decision.APROBAR, null));
    }

    private SolicitudAdminResponse rechazar(String motivo) {
        return servicio.responder(sesion, ID_SOLICITUD, new ResponderSolicitudRequest(Decision.RECHAZAR, motivo));
    }

    // ----- Cambio de fecha -----

    @Test
    void aprobarCambioDeFechaMueveLaCitaYLiberaElTurnoAnterior() {
        Solicitud solicitud = pendiente(cambioDeFecha());
        horariosBloqueados(anterior, nuevo);

        SolicitudAdminResponse respuesta = aprobar();

        assertSame(nuevo, cita.getHorario());
        assertEquals(Horario.RESERVADO, nuevo.getEstado());
        assertEquals(Horario.DISPONIBLE, anterior.getEstado());
        assertEquals("APROBADA", respuesta.estado());
        assertEquals("APROBADA", solicitud.getEstado());
        assertEquals(EN_DIEZ_DIAS.toString(), respuesta.fechaCita());
        assertTrue(respuesta.detalle().startsWith("Nueva fecha: "));
        assertNotNull(solicitud.getFechaRespuesta());
        assertEquals(ID_PERSONAL, solicitud.getIdPersonalRevisor());
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.CITA),
                eq("Tu cita fue reprogramada"), contains(DatosPrueba.TICKET));
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Solicitud aprobada"), anyString());
    }

    @Test
    void aprobarCambioConElTurnoAnteriorYaPasadoLoFinaliza() {
        anterior.setFecha(HOY.minusDays(1));
        pendiente(cambioDeFecha());
        horariosBloqueados(anterior, nuevo);

        aprobar();

        assertEquals(Horario.FINALIZADO, anterior.getEstado());
        assertSame(nuevo, cita.getHorario());
    }

    @Test
    void aprobarCambioConElTurnoNuevoYaTomadoResponde409SinMoverLaCita() {
        Solicitud solicitud = pendiente(cambioDeFecha());
        nuevo.setEstado(Horario.RESERVADO);
        horariosBloqueados(anterior, nuevo);

        exigeError(HttpStatus.CONFLICT, this::aprobar);

        assertSame(anterior, cita.getHorario());
        assertEquals(Horario.RESERVADO, anterior.getEstado());
        assertEquals(Solicitud.PENDIENTE, solicitud.getEstado());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void aprobarCambioConElTurnoNuevoEliminadoResponde409() {
        Solicitud solicitud = pendiente(cambioDeFecha());
        horariosBloqueados(anterior);

        exigeError(HttpStatus.CONFLICT, this::aprobar);

        assertSame(anterior, cita.getHorario());
        assertEquals(Solicitud.PENDIENTE, solicitud.getEstado());
    }

    @Test
    void rechazarCambioDeFechaDejaLaCitaDondeEstaYAvisaSoloAlDoctor() {
        Solicitud solicitud = pendiente(cambioDeFecha());

        SolicitudAdminResponse respuesta = rechazar("  Sin cupos ese día  ");

        assertEquals("RECHAZADA", respuesta.estado());
        assertEquals("Sin cupos ese día", respuesta.respuestaAdmin());
        assertEquals("Sin cupos ese día", solicitud.getRespuestaAdmin());
        assertSame(anterior, cita.getHorario());
        assertEquals(Horario.RESERVADO, anterior.getEstado());
        assertEquals(Horario.DISPONIBLE, nuevo.getEstado());
        verifyNoInteractions(horarioRepository);
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Solicitud rechazada"), contains("Sin cupos ese día"));
        verify(notificacionService, never()).crear(eq(paciente.getUsuario()), anyString(), anyString(), anyString());
    }

    // ----- Derivación -----

    @Test
    void aprobarDerivacionAvisaAlPacienteYAlDoctor() {
        Solicitud solicitud = pendiente(derivacion());

        SolicitudAdminResponse respuesta = aprobar();

        assertEquals("APROBADA", respuesta.estado());
        assertEquals("Hospital de Chosica · Cardiología", respuesta.detalle());
        assertEquals("APROBADA", solicitud.getEstado());
        assertSame(anterior, cita.getHorario());
        verifyNoInteractions(horarioRepository);
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Fuiste derivado a otro establecimiento"), contains("Hospital de Chosica"));
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Solicitud aprobada"), anyString());
    }

    @Test
    void rechazarDerivacionAvisaSoloAlDoctorConElMotivo() {
        pendiente(derivacion());

        SolicitudAdminResponse respuesta = rechazar("Completar el tratamiento primero");

        assertEquals("RECHAZADA", respuesta.estado());
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Solicitud rechazada"), contains("Completar el tratamiento primero"));
        verify(notificacionService, never()).crear(eq(paciente.getUsuario()), anyString(), anyString(), anyString());
    }

    // ----- Reglas comunes de las solicitudes -----

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazarSinMotivoResponde400YLaSolicitudSiguePendiente(String motivo) {
        Solicitud solicitud = pendiente(derivacion());

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> rechazar(motivo));

        assertEquals("Escribe el motivo del rechazo para el doctor.", error.getMessage());
        assertEquals(Solicitud.PENDIENTE, solicitud.getEstado());
        assertNull(solicitud.getFechaRespuesta());
        verifyNoInteractions(notificacionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADA", "RECHAZADA"})
    void solicitudYaRespondidaResponde409(String estado) {
        existeElPersonal();
        Solicitud solicitud = derivacion();
        solicitud.setEstado(estado);
        when(solicitudRepository.bloquear(ID_SOLICITUD)).thenReturn(Optional.of(solicitud));

        exigeError(HttpStatus.CONFLICT, this::aprobar);

        assertEquals(estado, solicitud.getEstado());
        verifyNoInteractions(citaRepository, notificacionService);
    }

    @Test
    void solicitudInexistenteResponde404() {
        existeElPersonal();
        when(solicitudRepository.bloquear(ID_SOLICITUD)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, this::aprobar);
    }

    @Test
    void aprobarConLaCitaYaCanceladaResponde409() {
        Solicitud solicitud = pendiente(cambioDeFecha());
        cita.setEstado(Cita.CANCELADA);

        exigeError(HttpStatus.CONFLICT, this::aprobar);

        assertEquals(Solicitud.PENDIENTE, solicitud.getEstado());
        verifyNoInteractions(horarioRepository);
    }

    @Test
    void rechazarConLaCitaYaCanceladaSiSePuede() {
        pendiente(cambioDeFecha());
        cita.setEstado(Cita.CANCELADA);

        assertEquals("RECHAZADA", rechazar("La cita ya no existe").estado());
    }

    @Test
    void otroRolNoPuedeResponderSolicitudes() {
        UsuarioSesion sesionDoctor = new UsuarioSesion(20, UsuarioSesion.DOCTOR);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.responder(sesionDoctor, ID_SOLICITUD,
                new ResponderSolicitudRequest(Decision.APROBAR, null)));

        verifyNoInteractions(personalRepository, solicitudRepository);
    }

    @Test
    void personalInactivoNoPuedeResponderSolicitudes() {
        personal.setEstado("INACTIVO");
        existeElPersonal();

        exigeError(HttpStatus.FORBIDDEN, this::aprobar);

        verifyNoInteractions(solicitudRepository);
    }

    // ----- Crear horarios -----

    private static CrearHorariosRequest turnos(LocalDate fecha, LocalTime inicio, LocalTime fin, int duracion) {
        return new CrearHorariosRequest(ID_DOCTOR, ID_CONSULTORIO, fecha, inicio, fin, duracion);
    }

    private void existeElDoctor() {
        when(doctorRepository.findById(ID_DOCTOR)).thenReturn(Optional.of(doctor));
    }

    private void generadorCrea(int creados, int omitidos) {
        when(horarioGenerador.crearTurnos(eq(doctor), any(Consultorio.class), any(LocalDate.class), any(LocalTime.class),
                any(LocalTime.class), anyInt())).thenReturn(new HorarioGeneradorService.Resultado(creados, omitidos));
    }

    private void exigeRechazoDeHorarios(CrearHorariosRequest datos, String mensaje) {
        existeElPersonal();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.crearHorarios(sesion, datos));

        assertEquals(mensaje, error.getMessage());
        verifyNoInteractions(doctorRepository, horarioGenerador);
    }

    @Test
    void crearHorariosDelegaLosTurnosYDevuelveElResumen() {
        existeElPersonal();
        existeElDoctor();
        when(horarioGenerador.crearTurnos(doctor, doctor.getConsultorios().get(0), EN_CINCO_DIAS, OCHO, DOCE, 30))
                .thenReturn(new HorarioGeneradorService.Resultado(6, 2));

        CrearHorariosResponse respuesta = servicio.crearHorarios(sesion, turnos(EN_CINCO_DIAS, OCHO, DOCE, 30));

        assertEquals(6, respuesta.creados());
        assertEquals(2, respuesta.omitidos());
        assertEquals(EN_CINCO_DIAS.toString(), respuesta.fecha());
        assertEquals("Ana Quispe", respuesta.doctor());
        assertEquals("Módulo A · Consultorio 1", respuesta.consultorio());
    }

    @Test
    void crearHorariosEnFechaPasadaResponde400() {
        exigeRechazoDeHorarios(turnos(HOY.minusDays(1), OCHO, DOCE, 30), "No se pueden crear horarios en fechas pasadas.");
    }

    @Test
    void crearHorariosMasAllaDeSeisMesesResponde400() {
        exigeRechazoDeHorarios(turnos(HOY.plusMonths(6).plusDays(1), OCHO, DOCE, 30),
                "Solo se pueden crear horarios hasta 6 meses adelante.");
    }

    @Test
    void crearHorariosJustoASeisMesesSePermite() {
        existeElPersonal();
        existeElDoctor();
        generadorCrea(8, 0);

        assertEquals(8, servicio.crearHorarios(sesion, turnos(HOY.plusMonths(6), OCHO, DOCE, 30)).creados());
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:00", "07:59"})
    void crearHorariosConFinIgualOAnteriorAlInicioResponde400(String fin) {
        exigeRechazoDeHorarios(turnos(EN_CINCO_DIAS, OCHO, LocalTime.parse(fin), 30),
                "La hora de fin debe ser posterior a la hora de inicio.");
    }

    @Test
    void crearHorariosConRangoMasCortoQueUnTurnoResponde400() {
        exigeRechazoDeHorarios(turnos(EN_CINCO_DIAS, OCHO, OCHO.plusMinutes(20), 30),
                "El rango de horas es más corto que un turno.");
    }

    @Test
    void crearMasDeCuarentaYOchoTurnosResponde400() {
        exigeRechazoDeHorarios(turnos(EN_CINCO_DIAS, OCHO, OCHO.plusMinutes(490), 10),
                "Puedes crear como máximo 48 turnos a la vez.");
    }

    @Test
    void crearExactamenteCuarentaYOchoTurnosSePermite() {
        existeElPersonal();
        existeElDoctor();
        generadorCrea(48, 0);

        assertEquals(48, servicio.crearHorarios(sesion, turnos(EN_CINCO_DIAS, OCHO, OCHO.plusMinutes(480), 10)).creados());
    }

    @Test
    void crearHorariosParaHoyConHoraYaPasadaResponde400() {
        // Medianoche siempre es igual o anterior a la hora actual.
        exigeRechazoDeHorarios(turnos(HOY, LocalTime.MIDNIGHT, LocalTime.of(1, 0), 30),
                "Para hoy, la hora de inicio debe ser posterior a la hora actual.");
    }

    @Test
    void crearHorariosParaDoctorInexistenteResponde404() {
        existeElPersonal();
        when(doctorRepository.findById(ID_DOCTOR)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.crearHorarios(sesion, turnos(EN_CINCO_DIAS, OCHO, DOCE, 30)));

        verifyNoInteractions(horarioGenerador);
    }

    @Test
    void crearHorariosParaDoctorInactivoResponde404() {
        existeElPersonal();
        doctor.setEstado("INACTIVO");
        existeElDoctor();

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.crearHorarios(sesion, turnos(EN_CINCO_DIAS, OCHO, DOCE, 30)));

        verifyNoInteractions(horarioGenerador);
    }

    @Test
    void crearHorariosEnConsultorioNoAsignadoResponde400() {
        existeElPersonal();
        existeElDoctor();
        CrearHorariosRequest datos = new CrearHorariosRequest(ID_DOCTOR, 99, EN_CINCO_DIAS, OCHO, DOCE, 30);

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.crearHorarios(sesion, datos));

        assertEquals("Ese consultorio no está asignado al doctor.", error.getMessage());
        verifyNoInteractions(horarioGenerador);
    }
}
