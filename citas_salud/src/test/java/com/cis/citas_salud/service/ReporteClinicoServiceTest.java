package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.PersonalAdministrativoRepository;
import com.cis.citas_salud.repository.ReporteClinicoRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import com.cis.citas_salud.service.ReporteClinicoService.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static com.cis.citas_salud.service.DatosPrueba.exigeError;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Documentos clínicos en PDF del doctor: quién puede generar cada uno y con qué plantilla. */
@ExtendWith(MockitoExtension.class)
class ReporteClinicoServiceTest {

    private static final int ID_ATENCION = 90;
    private static final int ID_CITA = 33;
    private static final byte[] PDF = {37, 80, 68, 70};

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private ReporteClinicoRepository reporteRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private AtencionRepository atencionRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private PersonalAdministrativoRepository personalRepository;
    @Mock
    private GeneradorReportesSql generador;

    private ReporteClinicoService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(20, UsuarioSesion.DOCTOR);
    private final UsuarioSesion sesionPaciente = new UsuarioSesion(10, UsuarioSesion.PACIENTE);
    private final UsuarioSesion sesionAdmin = new UsuarioSesion(30, UsuarioSesion.ADMINISTRADOR);
    private Doctor doctor;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        servicio = new ReporteClinicoService(doctorRepository, citaRepository, reporteRepository,
                pacienteRepository, atencionRepository, solicitudRepository, personalRepository, generador);
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
    }

    private void existeElDoctor() {
        when(doctorRepository.findByUsuario_IdUsuario(20)).thenReturn(Optional.of(doctor));
    }

    @ParameterizedTest
    @CsvSource({
            "FICHA_CONSULTA, FichaConsultaMedica, ficha-consulta-90.pdf",
            "INFORME_MEDICO, InformeMedico, informe-medico-90.pdf",
    })
    void elDoctorDescargaLosDocumentosDeSuAtencion(Documento documento, String plantilla, String nombre) {
        existeElDoctor();
        when(reporteRepository.esDelDoctor(ID_ATENCION, 3)).thenReturn(true);
        when(generador.pdf(plantilla, Map.of("ID_ATENCION", ID_ATENCION))).thenReturn(PDF);

        Archivo archivo = servicio.deAtencion(sesion, ID_ATENCION, documento);

        assertEquals(nombre, archivo.nombre());
        assertEquals("application/pdf", archivo.tipoMime());
        assertArrayEquals(PDF, archivo.contenido());
    }

    @Test
    void laReferenciaSeEmiteSiLaAtencionTieneDerivacion() {
        existeElDoctor();
        when(reporteRepository.esDelDoctor(ID_ATENCION, 3)).thenReturn(true);
        when(reporteRepository.tieneDerivacion(ID_ATENCION)).thenReturn(true);
        when(generador.pdf("InformeMedicoDerivacion", Map.of("ID_ATENCION", ID_ATENCION))).thenReturn(PDF);

        Archivo archivo = servicio.deAtencion(sesion, ID_ATENCION, Documento.REFERENCIA);

        assertEquals("referencia-90.pdf", archivo.nombre());
    }

    @Test
    void laReferenciaSinDerivacionResponde409() {
        existeElDoctor();
        when(reporteRepository.esDelDoctor(ID_ATENCION, 3)).thenReturn(true);
        when(reporteRepository.tieneDerivacion(ID_ATENCION)).thenReturn(false);

        ApiException error = exigeError(HttpStatus.CONFLICT,
                () -> servicio.deAtencion(sesion, ID_ATENCION, Documento.REFERENCIA));

        assertEquals("Esta atención no tiene una solicitud de derivación.", error.getMessage());
        verifyNoInteractions(generador);
    }

    @Test
    void laAtencionDeOtroDoctorResponde404() {
        existeElDoctor();
        when(reporteRepository.esDelDoctor(ID_ATENCION, 3)).thenReturn(false);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.deAtencion(sesion, ID_ATENCION, Documento.FICHA_CONSULTA));

        verifyNoInteractions(generador);
    }

    @Test
    void unPacienteNoGeneraDocumentosClinicos() {
        UsuarioSesion sesionPaciente = new UsuarioSesion(10, UsuarioSesion.PACIENTE);

        exigeError(HttpStatus.FORBIDDEN,
                () -> servicio.deAtencion(sesionPaciente, ID_ATENCION, Documento.INFORME_MEDICO));
        exigeError(HttpStatus.FORBIDDEN, () -> servicio.fichasDelPaciente(sesionPaciente, ID_CITA));

        verifyNoInteractions(doctorRepository, reporteRepository, generador);
    }

    @Test
    void lasFichasDelPacienteSalenDeUnaCitaDelDoctor() {
        existeElDoctor();
        Cita cita = DatosPrueba.cita(ID_CITA, paciente, DatosPrueba.horario(5, doctor, LocalDate.now()), Cita.CONFIRMADA);
        when(citaRepository.buscarConDetalle(ID_CITA)).thenReturn(Optional.of(cita));
        when(generador.pdf("FichaConsultaPaciente", Map.of("ID_PACIENTE", 1))).thenReturn(PDF);

        Archivo archivo = servicio.fichasDelPaciente(sesion, ID_CITA);

        assertEquals("fichas-consulta-12345678.pdf", archivo.nombre());
        assertArrayEquals(PDF, archivo.contenido());
    }

    private Cita citaAtendida(Paciente titular) {
        return DatosPrueba.cita(ID_CITA, titular,
                DatosPrueba.horario(5, doctor, LocalDate.now()), Cita.ATENDIDA);
    }

    private Atencion atencionDe(Cita cita) {
        Atencion atencion = new Atencion();
        atencion.setIdAtencion(ID_ATENCION);
        atencion.setCita(cita);
        return atencion;
    }

    @Test
    void pacienteDescargaSoloLaReferenciaDeSuCita() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
        when(reporteRepository.esDelPaciente(ID_ATENCION, 1)).thenReturn(true);
        when(reporteRepository.tieneDerivacion(ID_ATENCION)).thenReturn(true);
        when(generador.pdf("InformeMedicoDerivacion", Map.of("ID_ATENCION", ID_ATENCION))).thenReturn(PDF);

        Archivo archivo = servicio.deReferenciaPaciente(sesionPaciente, ID_ATENCION);

        assertEquals("referencia-90.pdf", archivo.nombre());
        assertArrayEquals(PDF, archivo.contenido());
    }

    @Test
    void pacienteNoPuedeDescargarLaReferenciaDeOtraPersona() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
        when(reporteRepository.esDelPaciente(ID_ATENCION, 1)).thenReturn(false);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.deReferenciaPaciente(sesionPaciente, ID_ATENCION));

        verifyNoInteractions(atencionRepository, generador);
    }

    @Test
    void pacienteNoDescargaUnaAtencionSinDerivacion() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.of(paciente));
        when(reporteRepository.esDelPaciente(ID_ATENCION, 1)).thenReturn(true);
        when(reporteRepository.tieneDerivacion(ID_ATENCION)).thenReturn(false);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.deReferenciaPaciente(sesionPaciente, ID_ATENCION));

        verifyNoInteractions(generador);
    }

    @Test
    void administracionDescargaLaReferenciaDesdeLaSolicitud() {
        PersonalAdministrativo admin = new PersonalAdministrativo();
        Cita cita = citaAtendida(paciente);
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo(Solicitud.DERIVACION);
        solicitud.setCita(cita);
        when(personalRepository.findByUsuario_IdUsuario(30)).thenReturn(Optional.of(admin));
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));
        when(atencionRepository.findByCita_IdCita(ID_CITA)).thenReturn(Optional.of(atencionDe(cita)));
        when(generador.pdf("InformeMedicoDerivacion", Map.of("ID_ATENCION", ID_ATENCION))).thenReturn(PDF);

        Archivo archivo = servicio.deReferenciaAdministracion(sesionAdmin, 7);

        assertEquals("referencia-90.pdf", archivo.nombre());
        assertArrayEquals(PDF, archivo.contenido());
    }

    @Test
    void pacienteSinPerfilNoPuedeDescargarReferencias() {
        when(pacienteRepository.findByUsuario_IdUsuario(10)).thenReturn(Optional.empty());

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.deReferenciaPaciente(sesionPaciente, ID_ATENCION));

        verifyNoInteractions(reporteRepository, generador);
    }

    @Test
    void administracionInactivaNoPuedeDescargarReferencias() {
        PersonalAdministrativo admin = new PersonalAdministrativo();
        admin.setEstado("INACTIVO");
        when(personalRepository.findByUsuario_IdUsuario(30)).thenReturn(Optional.of(admin));

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.deReferenciaAdministracion(sesionAdmin, 7));

        verifyNoInteractions(solicitudRepository, atencionRepository, generador);
    }

    @Test
    void administracionNoDescargaSiLaCitaAunNoTieneAtencion() {
        PersonalAdministrativo admin = new PersonalAdministrativo();
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo(Solicitud.DERIVACION);
        solicitud.setCita(citaAtendida(paciente));
        when(personalRepository.findByUsuario_IdUsuario(30)).thenReturn(Optional.of(admin));
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));
        when(atencionRepository.findByCita_IdCita(ID_CITA)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.deReferenciaAdministracion(sesionAdmin, 7));

        verifyNoInteractions(generador);
    }

    @Test
    void administracionNoDescargaSolicitudesQueNoSonDerivaciones() {
        PersonalAdministrativo admin = new PersonalAdministrativo();
        Solicitud solicitud = new Solicitud();
        solicitud.setTipo(Solicitud.CAMBIO_HORARIO);
        when(personalRepository.findByUsuario_IdUsuario(30)).thenReturn(Optional.of(admin));
        when(solicitudRepository.buscarConDetalle(7)).thenReturn(Optional.of(solicitud));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.deReferenciaAdministracion(sesionAdmin, 7));

        verifyNoInteractions(atencionRepository, generador);
    }

    @Test
    void rolNoAutorizadoNoConsultaLaReferencia() {
        exigeError(HttpStatus.FORBIDDEN,
                () -> servicio.deReferenciaAdministracion(sesionPaciente, 7));
        exigeError(HttpStatus.FORBIDDEN,
                () -> servicio.deReferenciaPaciente(sesion, ID_ATENCION));

        verifyNoInteractions(personalRepository, pacienteRepository, solicitudRepository, citaRepository,
                atencionRepository, reporteRepository, generador);
    }

    @Test
    void lasFichasDeUnaCitaDeOtroDoctorResponde404() {
        existeElDoctor();
        Doctor otro = DatosPrueba.doctor(4, DatosPrueba.usuario(21, UsuarioSesion.DOCTOR, "87654322"));
        Cita ajena = DatosPrueba.cita(ID_CITA, paciente, DatosPrueba.horario(5, otro, LocalDate.now()), Cita.CONFIRMADA);
        when(citaRepository.buscarConDetalle(ID_CITA)).thenReturn(Optional.of(ajena));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.fichasDelPaciente(sesion, ID_CITA));

        verifyNoInteractions(generador);
    }
}
