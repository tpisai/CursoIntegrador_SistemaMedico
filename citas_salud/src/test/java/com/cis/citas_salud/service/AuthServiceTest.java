package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AuthDto.LoginRequest;
import com.cis.citas_salud.api.dto.AuthDto.RegistroRequest;
import com.cis.citas_salud.api.dto.AuthDto.SesionResponse;
import com.cis.citas_salud.api.dto.AuthDto.TipoCuenta;
import com.cis.citas_salud.api.seguridad.TokenService;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.HistoriaClinica;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Rol;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.ConsultorioRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.RolRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** RF-01, RF-02 y RF-04: login con DNI + PIN, bloqueo por intentos y registro de cuentas. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DNI = "12345678";
    private static final String PIN = "123456";
    private static final String PIN_ERRADO = "999999";

    // Costo bajo: el resultado es el mismo y las pruebas no tardan.
    private final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder(4);
    private final TokenService tokenService = new TokenService("clave-solo-para-pruebas", new StandardEnvironment());

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private HistoriaClinicaRepository historiaClinicaRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private EspecialidadRepository especialidadRepository;
    @Mock
    private ConsultorioRepository consultorioRepository;
    @Mock
    private HorarioGeneradorService horarioGenerador;
    @Mock
    private NotificacionService notificacionService;

    private AuthService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarioRepository, rolRepository, pacienteRepository, historiaClinicaRepository,
                doctorRepository, especialidadRepository, consultorioRepository, horarioGenerador,
                notificacionService, tokenService, pinEncoder);
        usuario = DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, DNI);
        usuario.setPinHash(pinEncoder.encode(PIN));
    }

    private void existeElUsuario() {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(usuario));
    }

    private static LoginRequest login(String pin) {
        return new LoginRequest(DNI, pin);
    }

    private static RegistroRequest registroPaciente() {
        return new RegistroRequest(TipoCuenta.PACIENTE, " Ana ", " Quispe Rojas ", DNI, " Ana.Quispe@Correo.Test ",
                PIN, true, null, null);
    }

    private static RegistroRequest registroDoctor(Integer idEspecialidad, String cmp) {
        return new RegistroRequest(TipoCuenta.DOCTOR, "Rodrigo", "Mendoza Ríos", "87654321", "rodrigo@correo.test",
                PIN, true, idEspecialidad, cmp);
    }

    /** Sin duplicados y con el rol pedido: el punto de partida de un registro que sigue adelante. */
    private void registroDisponible(String rol, String dni, String correo) {
        when(usuarioRepository.existsByDni(dni)).thenReturn(false);
        when(usuarioRepository.existsByCorreoIgnoreCase(correo)).thenReturn(false);
        when(rolRepository.findByNombre(rol)).thenReturn(Optional.of(new Rol(rol)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(llamada -> {
            Usuario nuevo = llamada.getArgument(0);
            nuevo.setIdUsuario(50);
            return nuevo;
        });
    }

    // ----- Login -----

    @Test
    void loginCorrectoEntregaUnTokenValidoYLimpiaLosIntentos() {
        usuario.setIntentosFallidos(2);
        existeElUsuario();

        SesionResponse sesion = servicio.login(login(PIN));

        assertEquals(new UsuarioSesion(10, UsuarioSesion.PACIENTE), tokenService.validar(sesion.token()).orElseThrow());
        assertEquals(DNI, sesion.usuario().dni());
        assertEquals(UsuarioSesion.PACIENTE, sesion.usuario().rol());
        assertNull(sesion.usuario().especialidad());
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
    }

    @Test
    void loginDeDoctorIncluyeSuEspecialidad() {
        Usuario cuentaDoctor = DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, DNI);
        cuentaDoctor.setPinHash(pinEncoder.encode(PIN));
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(cuentaDoctor));
        when(doctorRepository.findByUsuario_IdUsuario(20)).thenReturn(Optional.of(DatosPrueba.doctor(3, cuentaDoctor)));

        assertEquals("Medicina General", servicio.login(login(PIN)).usuario().especialidad());
    }

    @Test
    void dniInexistenteResponde401() {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.empty());

        ApiException error = exigeError(HttpStatus.UNAUTHORIZED, () -> servicio.login(login(PIN)));

        assertEquals("DNI o PIN incorrectos.", error.getMessage());
    }

    @Test
    void primerPinIncorrectoCuentaElIntentoYAvisaCuantosQuedan() {
        existeElUsuario();

        ApiException error = exigeError(HttpStatus.UNAUTHORIZED, () -> servicio.login(login(PIN_ERRADO)));

        assertTrue(error.getMessage().endsWith("Te quedan 2 intentos."));
        assertEquals(1, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
    }

    @Test
    void segundoPinIncorrectoAvisaQueQuedaUnIntento() {
        usuario.setIntentosFallidos(1);
        existeElUsuario();

        ApiException error = exigeError(HttpStatus.UNAUTHORIZED, () -> servicio.login(login(PIN_ERRADO)));

        assertTrue(error.getMessage().endsWith("Te queda 1 intento."));
        assertEquals(2, usuario.getIntentosFallidos());
    }

    @Test
    void tercerPinIncorrectoBloqueaLaCuentaCincoMinutos() {
        usuario.setIntentosFallidos(2);
        existeElUsuario();
        LocalDateTime antes = LocalDateTime.now();

        ApiException error = exigeError(HttpStatus.LOCKED, () -> servicio.login(login(PIN_ERRADO)));

        assertTrue(error.getMessage().startsWith("Bloqueamos tu cuenta por 5 minutos"));
        assertEquals(0, usuario.getIntentosFallidos());
        assertFalse(usuario.getBloqueadoHasta().isBefore(antes.plusMinutes(5)));
        assertFalse(usuario.getBloqueadoHasta().isAfter(LocalDateTime.now().plusMinutes(5)));
    }

    @Test
    void cuentaBloqueadaResponde423AunqueElPinSeaCorrecto() {
        LocalDateTime bloqueo = LocalDateTime.now().plusMinutes(3);
        usuario.setBloqueadoHasta(bloqueo);
        existeElUsuario();

        ApiException error = exigeError(HttpStatus.LOCKED, () -> servicio.login(login(PIN)));

        assertTrue(error.getMessage().contains("bloqueada temporalmente"));
        assertEquals(bloqueo, usuario.getBloqueadoHasta());
    }

    @Test
    void bloqueoVencidoPermiteEntrarYSeLimpia() {
        usuario.setBloqueadoHasta(LocalDateTime.now().minusSeconds(1));
        existeElUsuario();

        servicio.login(login(PIN));

        assertNull(usuario.getBloqueadoHasta());
    }

    @Test
    void cuentaDesactivadaResponde403SinContarIntentos() {
        usuario.setEstado("INACTIVO");
        existeElUsuario();

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.login(login(PIN_ERRADO)));

        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void cuentaSinPinCuentaComoIntentoFallido() {
        usuario.setPinHash(null);
        existeElUsuario();

        exigeError(HttpStatus.UNAUTHORIZED, () -> servicio.login(login(PIN)));

        assertEquals(1, usuario.getIntentosFallidos());
    }

    // ----- Registro -----

    @Test
    void registroDePacienteCreaLaCuentaLaHistoriaYLaBienvenida() {
        registroDisponible(UsuarioSesion.PACIENTE, DNI, "Ana.Quispe@Correo.Test");
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(llamada -> {
            Paciente nuevo = llamada.getArgument(0);
            nuevo.setIdPaciente(12);
            return nuevo;
        });

        SesionResponse sesion = servicio.registrar(registroPaciente());

        assertEquals(new UsuarioSesion(50, UsuarioSesion.PACIENTE), tokenService.validar(sesion.token()).orElseThrow());
        assertEquals("ana.quispe@correo.test", sesion.usuario().correo());
        assertEquals("Ana", sesion.usuario().nombres());
        assertEquals("Quispe Rojas", sesion.usuario().apellidos());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertTrue(pinEncoder.matches(PIN, guardado.getValue().getPinHash()));
        assertTrue(guardado.getValue().getTerminosAceptados());

        ArgumentCaptor<HistoriaClinica> historia = ArgumentCaptor.forClass(HistoriaClinica.class);
        verify(historiaClinicaRepository).save(historia.capture());
        assertEquals("HC-000012", historia.getValue().getNumeroHistoria());
        verify(notificacionService).crear(any(Usuario.class), eq(Notificacion.SISTEMA), anyString(), anyString());
        verifyNoInteractions(doctorRepository);
    }

    @Test
    void registroConDniRepetidoResponde409SinCrearNada() {
        when(usuarioRepository.existsByDni(DNI)).thenReturn(true);

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.registrar(registroPaciente()));

        assertEquals("Ya existe una cuenta registrada con este DNI.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registroConCorreoRepetidoResponde409SinCrearNada() {
        when(usuarioRepository.existsByDni(DNI)).thenReturn(false);
        when(usuarioRepository.existsByCorreoIgnoreCase("Ana.Quispe@Correo.Test")).thenReturn(true);

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.registrar(registroPaciente()));

        assertEquals("Este correo ya está asociado a otra cuenta.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registroDeDoctorEligeElConsultorioActivoMenosOcupadoYGeneraSusHorarios() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");
        Especialidad especialidad = new Especialidad("Pediatría");
        when(doctorRepository.existsByCmp("045812")).thenReturn(false);
        when(especialidadRepository.findById(4)).thenReturn(Optional.of(especialidad));

        Consultorio lleno = DatosPrueba.consultorio(1);
        Consultorio conUno = DatosPrueba.consultorio(2);
        Consultorio libre = DatosPrueba.consultorio(3);
        Consultorio inactivo = DatosPrueba.consultorio(4);
        inactivo.setEstado("INACTIVO");
        when(consultorioRepository.findAll()).thenReturn(List.of(lleno, conUno, inactivo, libre));
        when(doctorRepository.findByEstado("ACTIVO")).thenReturn(List.of(
                doctorEn(lleno, conUno), doctorEn(lleno)));

        SesionResponse sesion = servicio.registrar(registroDoctor(4, "045812"));

        ArgumentCaptor<Doctor> doctor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(doctor.capture());
        assertEquals(List.of(libre), doctor.getValue().getConsultorios());
        assertSame(especialidad, doctor.getValue().getEspecialidad());
        assertEquals("045812", doctor.getValue().getCmp());
        verify(horarioGenerador).generarPara(doctor.getValue());
        verifyNoInteractions(pacienteRepository, historiaClinicaRepository);
        assertEquals(UsuarioSesion.DOCTOR, sesion.usuario().rol());
    }

    private static Doctor doctorEn(Consultorio... consultorios) {
        Doctor doctor = new Doctor();
        doctor.getConsultorios().addAll(List.of(consultorios));
        return doctor;
    }

    @Test
    void registroDeDoctorSinConsultoriosConEspacioQuedaSinConsultorio() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");
        when(doctorRepository.existsByCmp("045812")).thenReturn(false);
        when(especialidadRepository.findById(4)).thenReturn(Optional.of(new Especialidad("Pediatría")));
        Consultorio lleno = DatosPrueba.consultorio(1);
        when(consultorioRepository.findAll()).thenReturn(List.of(lleno));
        when(doctorRepository.findByEstado("ACTIVO")).thenReturn(List.of(doctorEn(lleno), doctorEn(lleno)));

        servicio.registrar(registroDoctor(4, "045812"));

        ArgumentCaptor<Doctor> doctor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(doctor.capture());
        assertTrue(doctor.getValue().getConsultorios().isEmpty());
    }

    @Test
    void registroDeDoctorSinEspecialidadResponde400() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.registrar(registroDoctor(null, "045812")));

        assertEquals("Elige tu especialidad.", error.getMessage());
        verify(doctorRepository, never()).save(any());
    }

    @Test
    void registroDeDoctorSinCmpResponde400() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.registrar(registroDoctor(4, " ")));

        assertEquals("Ingresa tu número de CMP.", error.getMessage());
    }

    @Test
    void registroDeDoctorConCmpRepetidoResponde409() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");
        when(doctorRepository.existsByCmp("045812")).thenReturn(true);

        exigeError(HttpStatus.CONFLICT, () -> servicio.registrar(registroDoctor(4, "045812")));

        verify(doctorRepository, never()).save(any());
    }

    @Test
    void registroDeDoctorConEspecialidadInexistenteResponde400() {
        registroDisponible(UsuarioSesion.DOCTOR, "87654321", "rodrigo@correo.test");
        when(doctorRepository.existsByCmp("045812")).thenReturn(false);
        when(especialidadRepository.findById(99)).thenReturn(Optional.empty());

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.registrar(registroDoctor(99, "045812")));

        verify(doctorRepository, never()).save(any());
    }
}
