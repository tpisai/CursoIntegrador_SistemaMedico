package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.DoctorDto.AtencionResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CambioFechaRequest;
import com.cis.citas_salud.api.dto.DoctorDto.DerivacionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.MedicamentoRequest;
import com.cis.citas_salud.api.dto.DoctorDto.RegistrarAtencionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.SolicitudResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.DetalleMedicamento;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.DocumentoMedico;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.HistoriaClinica;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DetalleMedicamentoRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
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

/** RF-10, RF-15 y RF-16: atención del paciente, documentos médicos y solicitudes a administración. */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    private static final int ID_CITA = 33;
    private static final int ID_ATENCION = 90;
    private static final LocalDate HOY = LocalDate.now();
    private static final byte[] CONTENIDO = "%PDF-1.4 receta".getBytes(StandardCharsets.UTF_8);

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AtencionRepository atencionRepository;
    @Mock
    private HistoriaClinicaRepository historiaRepository;
    @Mock
    private DocumentoMedicoRepository documentoRepository;
    @Mock
    private DetalleMedicamentoRepository detalleMedicamentoRepository;
    @Mock
    private NotificacionService notificacionService;
    @Captor
    private ArgumentCaptor<List<DetalleMedicamento>> medicamentosGuardados;

    @TempDir
    private Path carpetaArchivos;

    private DoctorService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(20, UsuarioSesion.DOCTOR);
    private Doctor doctor;
    private Paciente paciente;
    private HistoriaClinica historia;

    @BeforeEach
    void preparar() {
        servicio = new DoctorService(doctorRepository, citaRepository, horarioRepository, solicitudRepository,
                usuarioRepository, atencionRepository, historiaRepository, documentoRepository,
                detalleMedicamentoRepository, notificacionService, JsonMapper.builder().build(),
                carpetaArchivos.toString());
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        historia = new HistoriaClinica();
        historia.setPaciente(paciente);
    }

    private void existeElDoctor() {
        when(doctorRepository.findByUsuario_IdUsuario(20)).thenReturn(Optional.of(doctor));
    }

    private Cita citaDe(Doctor dueno, LocalDate fecha, String estado) {
        Horario horario = DatosPrueba.horario(5, dueno, fecha);
        horario.setEstado(Horario.RESERVADO);
        Cita cita = DatosPrueba.cita(ID_CITA, paciente, horario, estado);
        when(citaRepository.buscarConDetalle(ID_CITA)).thenReturn(Optional.of(cita));
        return cita;
    }

    private Doctor otroDoctor() {
        return DatosPrueba.doctor(4, DatosPrueba.usuario(21, UsuarioSesion.DOCTOR, "87654322"));
    }

    private void sinAtencionPrevia() {
        when(atencionRepository.existsByCita_IdCita(ID_CITA)).thenReturn(false);
    }

    private static RegistrarAtencionRequest atendido(String diagnostico, String tratamiento) {
        return atendidoConReceta(diagnostico, tratamiento, null);
    }

    private static RegistrarAtencionRequest atendidoConReceta(String diagnostico, String tratamiento,
                                                              List<MedicamentoRequest> medicamentos) {
        return new RegistrarAtencionRequest(true, " Dolor de garganta ", "  ", diagnostico, tratamiento, null,
                medicamentos);
    }

    private static RegistrarAtencionRequest noAsistio() {
        return new RegistrarAtencionRequest(false, null, null, null, null, null, null);
    }

    // ----- Registrar la atención -----

    @Test
    void registrarAtencionGuardaElDiagnosticoYCierraLaCita() {
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.of(historia));
        when(atencionRepository.save(any(Atencion.class))).thenAnswer(llamada -> {
            Atencion nueva = llamada.getArgument(0);
            nueva.setIdAtencion(ID_ATENCION);
            return nueva;
        });

        AtencionResponse respuesta = servicio.registrarAtencion(sesion, ID_CITA, atendido(" Faringitis ", " Paracetamol "));

        assertEquals(new AtencionResponse(ID_CITA, Cita.ATENDIDA, ID_ATENCION), respuesta);
        assertEquals(Cita.ATENDIDA, cita.getEstado());
        assertEquals(Horario.FINALIZADO, cita.getHorario().getEstado());

        ArgumentCaptor<Atencion> guardada = ArgumentCaptor.forClass(Atencion.class);
        verify(atencionRepository).save(guardada.capture());
        Atencion atencion = guardada.getValue();
        assertSame(historia, atencion.getHistoria());
        assertSame(cita, atencion.getCita());
        assertSame(doctor, atencion.getDoctor());
        assertEquals("FINALIZADA", atencion.getEstado());
        assertEquals("Faringitis", atencion.getDiagnostico());
        assertEquals("Paracetamol", atencion.getTratamiento());
        assertEquals("Dolor de garganta", atencion.getAnamnesis());
        assertNull(atencion.getExamenFisico());
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.CITA),
                eq("Atención registrada"), anyString());
        verifyNoInteractions(detalleMedicamentoRepository);
    }

    @Test
    void registrarAtencionGuardaLosMedicamentosDeLaReceta() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.of(historia));
        List<MedicamentoRequest> receta = List.of(
                new MedicamentoRequest(" Amoxicilina 500 mg ", "1 cápsula", "21", "Oral", "Cápsulas", "1",
                        "Cada 8 horas", "7 días", null),
                new MedicamentoRequest("Paracetamol 500 mg", "  ", null, null, null, null, "Si hay fiebre", null, 5));

        servicio.registrarAtencion(sesion, ID_CITA, atendidoConReceta("Faringitis", "Antibiótico", receta));

        verify(detalleMedicamentoRepository).saveAll(medicamentosGuardados.capture());
        List<DetalleMedicamento> guardados = medicamentosGuardados.getValue();
        assertEquals(2, guardados.size());
        assertEquals("Amoxicilina 500 mg", guardados.get(0).getNombreMedicamento());
        assertEquals("Cada 8 horas", guardados.get(0).getFrecuencia());
        assertEquals(1, guardados.get(0).getOrden());
        // Un campo en blanco se guarda vacío y el orden indicado se respeta.
        assertNull(guardados.get(1).getDosis());
        assertEquals(5, guardados.get(1).getOrden());
        assertEquals("Faringitis", guardados.get(1).getAtencion().getDiagnostico());
    }

    @Test
    void registrarAtencionDeUnaCitaDeDiasAnterioresSePermite() {
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY.minusDays(2), Cita.RESERVADA);
        sinAtencionPrevia();
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.of(historia));

        servicio.registrarAtencion(sesion, ID_CITA, atendido("Faringitis", "Paracetamol"));

        assertEquals(Cita.ATENDIDA, cita.getEstado());
    }

    @Test
    void registrarInasistenciaCierraLaCitaSinCrearAtencion() {
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();

        AtencionResponse respuesta = servicio.registrarAtencion(sesion, ID_CITA, noAsistio());

        assertEquals(new AtencionResponse(ID_CITA, Cita.NO_ASISTIO, null), respuesta);
        assertEquals(Cita.NO_ASISTIO, cita.getEstado());
        assertEquals(Horario.FINALIZADO, cita.getHorario().getEstado());
        verify(atencionRepository, never()).save(any());
        verifyNoInteractions(historiaRepository, notificacionService);
    }

    @Test
    void atencionRepetidaResponde409() {
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY, Cita.CONFIRMADA);
        when(atencionRepository.existsByCita_IdCita(ID_CITA)).thenReturn(true);

        ApiException error = exigeError(HttpStatus.CONFLICT,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido("Faringitis", "Paracetamol")));

        assertEquals("Esta cita ya tiene una atención registrada.", error.getMessage());
        assertEquals(Cita.CONFIRMADA, cita.getEstado());
        assertEquals(Horario.RESERVADO, cita.getHorario().getEstado());
        verify(atencionRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {Cita.ATENDIDA, Cita.NO_ASISTIO, Cita.CANCELADA})
    void citaQueYaNoEstaActivaResponde409(String estado) {
        existeElDoctor();
        citaDe(doctor, HOY, estado);

        ApiException error = exigeError(HttpStatus.CONFLICT,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido("Faringitis", "Paracetamol")));

        assertEquals("Esa cita ya no está activa.", error.getMessage());
        verify(atencionRepository, never()).save(any());
    }

    @Test
    void citaDeOtroDoctorResponde404SinTocarla() {
        existeElDoctor();
        Cita cita = citaDe(otroDoctor(), HOY, Cita.CONFIRMADA);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.registrarAtencion(sesion, ID_CITA, noAsistio()));

        assertEquals(Cita.CONFIRMADA, cita.getEstado());
        verifyNoInteractions(atencionRepository);
    }

    @Test
    void citaDeUnDiaFuturoResponde409() {
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY.plusDays(1), Cita.CONFIRMADA);

        exigeError(HttpStatus.CONFLICT, () -> servicio.registrarAtencion(sesion, ID_CITA, noAsistio()));

        assertEquals(Cita.CONFIRMADA, cita.getEstado());
        verifyNoInteractions(atencionRepository);
    }

    @Test
    void atencionSinDiagnosticoResponde400() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido("  ", "Paracetamol")));

        assertEquals("Escribe el diagnóstico.", error.getMessage());
        verify(atencionRepository, never()).save(any());
    }

    @Test
    void atencionSinTratamientoResponde400() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido("Faringitis", null)));

        assertEquals("Escribe el tratamiento indicado.", error.getMessage());
    }

    @Test
    void apoyoAlDiagnosticoRegistraLaConclusionSinTratamiento() {
        doctor.getEspecialidad().setTipo(Especialidad.APOYO_DIAGNOSTICO);
        existeElDoctor();
        Cita cita = citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.of(historia));

        servicio.registrarAtencion(sesion, ID_CITA, atendido("Hemograma dentro de los valores de referencia.", null));

        assertEquals(Cita.ATENDIDA, cita.getEstado());
        ArgumentCaptor<Atencion> guardada = ArgumentCaptor.forClass(Atencion.class);
        verify(atencionRepository).save(guardada.capture());
        assertEquals("Hemograma dentro de los valores de referencia.", guardada.getValue().getDiagnostico());
        assertNull(guardada.getValue().getTratamiento());
    }

    @Test
    void apoyoAlDiagnosticoSinConclusionResponde400() {
        doctor.getEspecialidad().setTipo(Especialidad.APOYO_DIAGNOSTICO);
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido(" ", null)));

        assertEquals("Escribe la conclusión del examen.", error.getMessage());
        verify(atencionRepository, never()).save(any());
    }

    @Test
    void pacienteSinHistoriaClinicaResponde409() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinAtencionPrevia();
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.empty());

        exigeError(HttpStatus.CONFLICT,
                () -> servicio.registrarAtencion(sesion, ID_CITA, atendido("Faringitis", "Paracetamol")));

        verify(atencionRepository, never()).save(any());
    }

    @Test
    void otroRolNoPuedeRegistrarAtenciones() {
        UsuarioSesion sesionPaciente = new UsuarioSesion(10, UsuarioSesion.PACIENTE);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.registrarAtencion(sesionPaciente, ID_CITA, noAsistio()));

        verifyNoInteractions(doctorRepository, citaRepository);
    }

    // ----- Documentos médicos -----

    private void existeLaAtencionDe(Doctor dueno) {
        Atencion atencion = new Atencion();
        atencion.setIdAtencion(ID_ATENCION);
        atencion.setDoctor(dueno);
        atencion.setHistoria(historia);
        when(atencionRepository.findById(ID_ATENCION)).thenReturn(Optional.of(atencion));
    }

    private static MockMultipartFile archivo(String nombre, String tipo) {
        return new MockMultipartFile("archivo", nombre, tipo, CONTENIDO);
    }

    private void noSeGuardoNingunArchivo() throws IOException {
        verify(documentoRepository, never()).save(any());
        try (var contenido = Files.list(carpetaArchivos)) {
            assertEquals(0, contenido.count());
        }
    }

    @Test
    void subirDocumentoGuardaElArchivoYAvisaAlPaciente() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        DocumentoResponse respuesta = servicio.subirDocumento(sesion, ID_ATENCION, "Receta médica",
                archivo("receta.pdf", "application/pdf"));

        assertEquals("Receta médica", respuesta.tipoDocumento());
        assertEquals("receta.pdf", respuesta.nombreArchivo());
        assertEquals(HOY.toString(), respuesta.fechaCarga());

        ArgumentCaptor<DocumentoMedico> guardado = ArgumentCaptor.forClass(DocumentoMedico.class);
        verify(documentoRepository).save(guardado.capture());
        DocumentoMedico documento = guardado.getValue();
        assertEquals("application/pdf", documento.getTipoMime());
        assertTrue(documento.getRutaArchivo().startsWith("atenciones/" + ID_ATENCION + "/"));
        assertArrayEquals(CONTENIDO, Files.readAllBytes(carpetaArchivos.resolve(documento.getRutaArchivo())));
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.SISTEMA),
                eq("Nuevo documento disponible"), contains("Receta médica"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png"})
    void tambienSeAceptanImagenes(String tipo) {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        assertEquals("foto.img", servicio.subirDocumento(sesion, ID_ATENCION, "Otro", archivo("foto.img", tipo)).nombreArchivo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/zip", "application/x-msdownload", "image/gif"})
    void tipoDeArchivoNoPermitidoResponde400(String tipo) throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.subirDocumento(sesion, ID_ATENCION, "Receta médica", archivo("receta.pdf", tipo)));

        assertEquals("Solo se aceptan archivos PDF, JPG o PNG.", error.getMessage());
        noSeGuardoNingunArchivo();
    }

    @Test
    void nombreDeArchivoConRutaNoSaleDeLaCarpetaDeDocumentos() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        DocumentoResponse respuesta = servicio.subirDocumento(sesion, ID_ATENCION, "Otro",
                archivo("../../../fuera de carpeta.pdf", "application/pdf"));

        assertEquals("fuera_de_carpeta.pdf", respuesta.nombreArchivo());
        ArgumentCaptor<DocumentoMedico> guardado = ArgumentCaptor.forClass(DocumentoMedico.class);
        verify(documentoRepository).save(guardado.capture());
        Path destino = carpetaArchivos.resolve(guardado.getValue().getRutaArchivo()).normalize();
        assertTrue(destino.startsWith(carpetaArchivos));
        assertTrue(Files.isRegularFile(destino));
    }

    @Test
    void tipoDeDocumentoDesconocidoResponde400() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.subirDocumento(sesion, ID_ATENCION, "Selfie", archivo("receta.pdf", "application/pdf")));

        noSeGuardoNingunArchivo();
    }

    @Test
    void archivoVacioResponde400() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);
        MockMultipartFile vacio = new MockMultipartFile("archivo", "receta.pdf", "application/pdf", new byte[0]);

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.subirDocumento(sesion, ID_ATENCION, "Otro", vacio));

        noSeGuardoNingunArchivo();
    }

    @Test
    void atencionDeOtroDoctorResponde404() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(otroDoctor());

        exigeError(HttpStatus.NOT_FOUND,
                () -> servicio.subirDocumento(sesion, ID_ATENCION, "Otro", archivo("receta.pdf", "application/pdf")));

        noSeGuardoNingunArchivo();
    }

    // ----- Casos que antes fallaban -----

    @Test
    void archivoSinContentTypeDeberiaResponder400() throws IOException {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        ApiException error = exigeError(HttpStatus.BAD_REQUEST,
                () -> servicio.subirDocumento(sesion, ID_ATENCION, "Otro", archivo("receta.pdf", null)));

        assertEquals("Solo se aceptan archivos PDF, JPG o PNG.", error.getMessage());
        noSeGuardoNingunArchivo();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "informe?.pdf", "control: marzo.pdf"})
    void nombreDeArchivoRaroDeberiaLimpiarseEnLugarDeFallar(String nombre) {
        existeElDoctor();
        existeLaAtencionDe(doctor);

        DocumentoResponse respuesta = servicio.subirDocumento(sesion, ID_ATENCION, "Otro", archivo(nombre, "application/pdf"));

        assertFalse(respuesta.nombreArchivo().isBlank());
    }

    // ----- Solicitudes a administración -----

    private void sinSolicitudPendiente(String tipo) {
        when(solicitudRepository.existsByCita_IdCitaAndTipoAndEstado(ID_CITA, tipo, Solicitud.PENDIENTE)).thenReturn(false);
    }

    private void solicitudSeGuardaConId() {
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(llamada -> {
            Solicitud nueva = llamada.getArgument(0);
            nueva.setIdSolicitud(7);
            return nueva;
        });
    }

    private Horario turnoLibre(Doctor dueno) {
        Horario horario = DatosPrueba.horario(21, dueno, HOY.plusDays(10));
        when(horarioRepository.findById(21)).thenReturn(Optional.of(horario));
        return horario;
    }

    private static CambioFechaRequest cambioFecha() {
        return new CambioFechaRequest(ID_CITA, 21, "  Reunión clínica  ");
    }

    @Test
    void solicitarCambioDeFechaGuardaElTurnoPropuestoYAvisaAAdministracion() {
        existeElDoctor();
        citaDe(doctor, HOY.plusDays(5), Cita.CONFIRMADA);
        Horario propuesto = turnoLibre(doctor);
        sinSolicitudPendiente(Solicitud.CAMBIO_HORARIO);
        solicitudSeGuardaConId();
        Usuario admin = DatosPrueba.usuario(1, UsuarioSesion.ADMINISTRADOR, "44556677");
        when(usuarioRepository.findByRol_NombreAndEstado("ADMINISTRADOR", "ACTIVO")).thenReturn(List.of(admin));

        SolicitudResponse respuesta = servicio.solicitarCambioFecha(sesion, cambioFecha());

        assertEquals(7, respuesta.idSolicitud());
        assertEquals(Solicitud.CAMBIO_HORARIO, respuesta.tipo());
        assertEquals(Solicitud.PENDIENTE, respuesta.estado());
        assertEquals("Reunión clínica", respuesta.motivo());
        assertTrue(respuesta.detalle().startsWith("Nueva fecha: "));

        ArgumentCaptor<Solicitud> guardada = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(guardada.capture());
        assertTrue(guardada.getValue().getDatosAdicionales().contains("\"idHorarioNuevo\":21"));
        // El turno sigue libre hasta que administración apruebe.
        assertEquals(Horario.DISPONIBLE, propuesto.getEstado());
        verify(notificacionService).crear(eq(admin), eq(Notificacion.SOLICITUD), eq("Nueva solicitud por revisar"), anyString());
    }

    @Test
    void cambioDeFechaHaciaUnTurnoOcupadoResponde409() {
        existeElDoctor();
        citaDe(doctor, HOY.plusDays(5), Cita.CONFIRMADA);
        turnoLibre(doctor).setEstado(Horario.RESERVADO);

        exigeError(HttpStatus.CONFLICT, () -> servicio.solicitarCambioFecha(sesion, cambioFecha()));

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void cambioDeFechaHaciaUnTurnoDeOtroDoctorResponde404() {
        existeElDoctor();
        citaDe(doctor, HOY.plusDays(5), Cita.CONFIRMADA);
        turnoLibre(otroDoctor());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.solicitarCambioFecha(sesion, cambioFecha()));

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void cambioDeFechaYaPendienteResponde409() {
        existeElDoctor();
        citaDe(doctor, HOY.plusDays(5), Cita.CONFIRMADA);
        turnoLibre(doctor);
        when(solicitudRepository.existsByCita_IdCitaAndTipoAndEstado(ID_CITA, Solicitud.CAMBIO_HORARIO, Solicitud.PENDIENTE))
                .thenReturn(true);

        exigeError(HttpStatus.CONFLICT, () -> servicio.solicitarCambioFecha(sesion, cambioFecha()));

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void derivarGuardaElDestinoLegible() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        sinSolicitudPendiente(Solicitud.DERIVACION);
        solicitudSeGuardaConId();
        when(usuarioRepository.findByRol_NombreAndEstado("ADMINISTRADOR", "ACTIVO")).thenReturn(List.of());

        SolicitudResponse respuesta = servicio.derivar(sesion,
                new DerivacionRequest(ID_CITA, " Hospital de Chosica ", " Cardiología ", "Soplo cardíaco"));

        assertEquals(Solicitud.DERIVACION, respuesta.tipo());
        assertEquals("Hospital de Chosica · Cardiología", respuesta.detalle());
        verify(notificacionService).crear(eq(doctor.getUsuario()), eq(Notificacion.SOLICITUD),
                eq("Derivación enviada a administración"), anyString());
    }

    @Test
    void derivacionYaPendienteResponde409() {
        existeElDoctor();
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        when(solicitudRepository.existsByCita_IdCitaAndTipoAndEstado(ID_CITA, Solicitud.DERIVACION, Solicitud.PENDIENTE))
                .thenReturn(true);

        exigeError(HttpStatus.CONFLICT, () -> servicio.derivar(sesion,
                new DerivacionRequest(ID_CITA, "Hospital", "Cardiología", "Soplo")));

        verify(solicitudRepository, never()).save(any());
    }
}
