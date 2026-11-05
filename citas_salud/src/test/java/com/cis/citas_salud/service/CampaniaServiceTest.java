package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.CampaniaAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.CrearCampaniaRequest;
import com.cis.citas_salud.api.dto.CampaniaDto.InscripcionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Campania;
import com.cis.citas_salud.entity.InscripcionCampania;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.CampaniaRepository;
import com.cis.citas_salud.repository.InscripcionCampaniaRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF-19 a RF-21: publicación de campañas e inscripción de pacientes. */
@ExtendWith(MockitoExtension.class)
class CampaniaServiceTest {

    private static final int ID_CAMPANIA = 3;
    private static final int ID_PACIENTE = 1;
    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private CampaniaRepository campaniaRepository;
    @Mock
    private InscripcionCampaniaRepository inscripcionRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private AdminService adminService;
    @Mock
    private NotificacionService notificacionService;

    private CampaniaService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(10, UsuarioSesion.PACIENTE);
    private final UsuarioSesion sesionAdmin = new UsuarioSesion(1, UsuarioSesion.ADMINISTRADOR);
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        servicio = new CampaniaService(campaniaRepository, inscripcionRepository, pacienteRepository, adminService,
                notificacionService);
        paciente = DatosPrueba.paciente(ID_PACIENTE, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
    }

    private void existeElPaciente() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
    }

    private Campania campania(String estado, LocalDate fin) {
        Campania campania = new Campania();
        campania.setIdCampania(ID_CAMPANIA);
        campania.setTitulo("Vacunación contra la influenza");
        campania.setEstado(estado);
        campania.setFechaInicio(fin.minusDays(2));
        campania.setFechaFin(fin);
        campania.setCupos(10);
        campania.setCuposDisponibles(10);
        when(campaniaRepository.findById(ID_CAMPANIA)).thenReturn(Optional.of(campania));
        return campania;
    }

    private void noEstaInscrito() {
        when(inscripcionRepository.existsByCampania_IdCampaniaAndPaciente_IdPaciente(ID_CAMPANIA, ID_PACIENTE))
                .thenReturn(false);
    }

    private void inscripcionSeGuardaConId(int id) {
        when(inscripcionRepository.saveAndFlush(any(InscripcionCampania.class))).thenAnswer(llamada -> {
            InscripcionCampania nueva = llamada.getArgument(0);
            nueva.setIdInscripcion(id);
            return nueva;
        });
    }

    // ----- Inscripción -----

    @Test
    void inscribirTomaUnCupoYEntregaElComprobante() {
        existeElPaciente();
        Campania campania = campania(Campania.ACTIVA, HOY.plusDays(5));
        noEstaInscrito();
        when(campaniaRepository.tomarCupo(ID_CAMPANIA)).thenReturn(1);
        inscripcionSeGuardaConId(12);

        InscripcionResponse respuesta = servicio.inscribir(sesion, ID_CAMPANIA);

        assertEquals(12, respuesta.idInscripcion());
        assertEquals("C-000012", respuesta.codigo());
        assertEquals(ID_CAMPANIA, respuesta.idCampania());
        assertEquals(campania.getTitulo(), respuesta.campania());

        ArgumentCaptor<InscripcionCampania> guardada = ArgumentCaptor.forClass(InscripcionCampania.class);
        verify(inscripcionRepository).saveAndFlush(guardada.capture());
        assertSame(paciente, guardada.getValue().getPaciente());
        assertSame(campania, guardada.getValue().getCampania());
        assertEquals(InscripcionCampania.INSCRITO, guardada.getValue().getEstado());
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.CAMPANIA), contains("C-000012"), anyString());
    }

    @Test
    void campaniaPlanificadaTambienAceptaInscripciones() {
        existeElPaciente();
        campania(Campania.PLANIFICADA, HOY.plusDays(20));
        noEstaInscrito();
        when(campaniaRepository.tomarCupo(ID_CAMPANIA)).thenReturn(1);
        inscripcionSeGuardaConId(13);

        assertEquals("C-000013", servicio.inscribir(sesion, ID_CAMPANIA).codigo());
    }

    @Test
    void campaniaQueTerminaHoyAunAceptaInscripciones() {
        existeElPaciente();
        campania(Campania.ACTIVA, HOY);
        noEstaInscrito();
        when(campaniaRepository.tomarCupo(ID_CAMPANIA)).thenReturn(1);
        inscripcionSeGuardaConId(14);

        assertEquals("C-000014", servicio.inscribir(sesion, ID_CAMPANIA).codigo());
    }

    @Test
    void inscripcionDuplicadaResponde409SinGastarCupo() {
        existeElPaciente();
        campania(Campania.ACTIVA, HOY.plusDays(5));
        when(inscripcionRepository.existsByCampania_IdCampaniaAndPaciente_IdPaciente(ID_CAMPANIA, ID_PACIENTE))
                .thenReturn(true);

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.inscribir(sesion, ID_CAMPANIA));

        assertEquals("Ya estás inscrito en esta campaña.", error.getMessage());
        verify(campaniaRepository, never()).tomarCupo(anyInt());
        verify(inscripcionRepository, never()).saveAndFlush(any());
    }

    @Test
    void sinCuposResponde409SinInscribir() {
        existeElPaciente();
        campania(Campania.ACTIVA, HOY.plusDays(5));
        noEstaInscrito();
        when(campaniaRepository.tomarCupo(ID_CAMPANIA)).thenReturn(0);

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.inscribir(sesion, ID_CAMPANIA));

        assertEquals("Ya no quedan cupos en esta campaña.", error.getMessage());
        verify(inscripcionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void campaniaVencidaResponde404SinTomarCupo() {
        existeElPaciente();
        campania(Campania.ACTIVA, HOY.minusDays(1));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.inscribir(sesion, ID_CAMPANIA));

        verify(campaniaRepository, never()).tomarCupo(anyInt());
        verifyNoInteractions(inscripcionRepository);
    }

    @Test
    void campaniaFinalizadaResponde404AunqueSuFechaNoHayaPasado() {
        existeElPaciente();
        campania("FINALIZADA", HOY.plusDays(5));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.inscribir(sesion, ID_CAMPANIA));

        verify(campaniaRepository, never()).tomarCupo(anyInt());
    }

    @Test
    void campaniaInexistenteResponde404() {
        existeElPaciente();
        when(campaniaRepository.findById(ID_CAMPANIA)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.inscribir(sesion, ID_CAMPANIA));
    }

    @Test
    void dobleClicSimultaneoResponde409CuandoLaBaseRechazaElDuplicado() {
        existeElPaciente();
        campania(Campania.ACTIVA, HOY.plusDays(5));
        noEstaInscrito();
        when(campaniaRepository.tomarCupo(ID_CAMPANIA)).thenReturn(1);
        when(inscripcionRepository.saveAndFlush(any(InscripcionCampania.class)))
                .thenThrow(new DataIntegrityViolationException("uq_campania_paciente"));

        ApiException error = exigeError(HttpStatus.CONFLICT, () -> servicio.inscribir(sesion, ID_CAMPANIA));

        assertEquals("Ya estás inscrito en esta campaña.", error.getMessage());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void unDoctorNoPuedeInscribirseResponde403() {
        UsuarioSesion sesionDoctor = new UsuarioSesion(20, UsuarioSesion.DOCTOR);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.inscribir(sesionDoctor, ID_CAMPANIA));

        verifyNoInteractions(campaniaRepository, inscripcionRepository);
    }

    // ----- Publicación -----

    private static CrearCampaniaRequest peticion(LocalDate inicio, LocalDate fin) {
        return new CrearCampaniaRequest("  Charla de nutrición  ", "   ", inicio, fin, null, " Auditorio ", 30);
    }

    private PersonalAdministrativo existeElAdmin() {
        PersonalAdministrativo admin = new PersonalAdministrativo();
        when(adminService.personal(sesionAdmin)).thenReturn(admin);
        return admin;
    }

    @Test
    void crearCampaniaFuturaQuedaPlanificadaYAvisaALosPacientes() {
        PersonalAdministrativo admin = existeElAdmin();
        Usuario ana = DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678");
        Usuario luis = DatosPrueba.usuario(11, UsuarioSesion.PACIENTE, "45879621");
        when(pacienteRepository.usuariosActivos()).thenReturn(List.of(ana, luis));

        CampaniaAdminResponse respuesta = servicio.crear(sesionAdmin, peticion(HOY.plusDays(3), HOY.plusDays(4)));

        assertEquals(Campania.PLANIFICADA, respuesta.estado());
        assertEquals("Charla de nutrición", respuesta.titulo());
        assertNull(respuesta.descripcion());
        assertEquals("Auditorio", respuesta.lugar());
        assertEquals(30, respuesta.cupos());
        assertEquals(30, respuesta.cuposDisponibles());
        assertEquals(0, respuesta.inscritos());

        ArgumentCaptor<Campania> guardada = ArgumentCaptor.forClass(Campania.class);
        verify(campaniaRepository).save(guardada.capture());
        assertSame(admin, guardada.getValue().getPersonal());
        verify(notificacionService).crear(eq(ana), eq(Notificacion.CAMPANIA), contains("Charla de nutrición"), anyString());
        verify(notificacionService).crear(eq(luis), eq(Notificacion.CAMPANIA), contains("Charla de nutrición"), anyString());
    }

    @Test
    void crearCampaniaQueEmpiezaHoyQuedaActiva() {
        existeElAdmin();
        when(pacienteRepository.usuariosActivos()).thenReturn(List.of());

        assertEquals(Campania.ACTIVA, servicio.crear(sesionAdmin, peticion(HOY, HOY)).estado());
    }

    @Test
    void crearCampaniaConFinAntesDelInicioResponde400() {
        existeElAdmin();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(sesionAdmin, peticion(HOY.plusDays(3), HOY.plusDays(2))));

        assertEquals("La fecha de fin no puede ser anterior a la de inicio.", error.getMessage());
        verify(campaniaRepository, never()).save(any());
    }

    @Test
    void crearCampaniaQueTerminaEnElPasadoResponde400() {
        existeElAdmin();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(sesionAdmin, peticion(HOY.minusDays(3), HOY.minusDays(1))));

        assertEquals("La campaña no puede terminar en el pasado.", error.getMessage());
        verify(campaniaRepository, never()).save(any());
    }
}
