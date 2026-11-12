package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.ReporteClinicoRepository;
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
    private GeneradorReportesSql generador;

    private ReporteClinicoService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(20, UsuarioSesion.DOCTOR);
    private Doctor doctor;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        servicio = new ReporteClinicoService(doctorRepository, citaRepository, reporteRepository, generador);
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
