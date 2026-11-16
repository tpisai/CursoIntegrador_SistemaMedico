package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.ConsultaDto.AntecedentesRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.AtencionPreviaResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.ConsultaResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.PacienteResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.ReporteExamenesRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.ResultadoExamenRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.ResultadoExamenResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
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
import com.cis.citas_salud.entity.ResultadoExamen;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.HistorialClinicoRepository;
import com.cis.citas_salud.repository.ReporteClinicoRepository;
import com.cis.citas_salud.repository.ResultadoExamenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Página de consulta del doctor: agenda por fechas, historia clínica del paciente y reporte de exámenes. */
@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    private static final int ID_CITA = 33;
    private static final int ID_HISTORIA = 7;
    private static final LocalDate HOY = LocalDate.now();
    private static final List<String> EN_AGENDA =
            List.of(Cita.RESERVADA, Cita.CONFIRMADA, Cita.ATENDIDA, Cita.NO_ASISTIO);

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HistoriaClinicaRepository historiaRepository;
    @Mock
    private HistorialClinicoRepository historialRepository;
    @Mock
    private AtencionRepository atencionRepository;
    @Mock
    private DocumentoMedicoRepository documentoRepository;
    @Mock
    private ResultadoExamenRepository resultadoRepository;
    @Mock
    private ReporteClinicoRepository reporteRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private CitaService citaService;
    @Captor
    private ArgumentCaptor<List<ResultadoExamen>> resultadosGuardados;

    private ConsultaService servicio;
    private final UsuarioSesion sesion = new UsuarioSesion(20, UsuarioSesion.DOCTOR);
    private Doctor doctor;
    private Doctor laboratorio;
    private Paciente paciente;
    private HistoriaClinica historia;

    @BeforeEach
    void preparar() {
        servicio = new ConsultaService(doctorRepository, citaRepository, historiaRepository, historialRepository,
                atencionRepository, documentoRepository, resultadoRepository, reporteRepository, notificacionService,
                citaService);
        doctor = DatosPrueba.doctor(3, DatosPrueba.usuario(20, UsuarioSesion.DOCTOR, "87654321"));
        laboratorio = DatosPrueba.doctor(4, DatosPrueba.usuario(21, UsuarioSesion.DOCTOR, "87654343"));
        laboratorio.getEspecialidad().setNombre("Laboratorio");
        laboratorio.getEspecialidad().setTipo(Especialidad.APOYO_DIAGNOSTICO);
        paciente = DatosPrueba.paciente(1, DatosPrueba.usuario(10, UsuarioSesion.PACIENTE, "12345678"));
        paciente.setFechaNacimiento(HOY.minusYears(28).minusDays(1));
        historia = new HistoriaClinica();
        historia.setIdHistoria(ID_HISTORIA);
        historia.setPaciente(paciente);
        historia.setNumeroHistoria("HC-000001");
    }

    private void entra(Doctor quien) {
        when(doctorRepository.findByUsuario_IdUsuario(20)).thenReturn(Optional.of(quien));
    }

    private Cita citaDe(Doctor dueno, LocalDate fecha, String estado) {
        Horario horario = DatosPrueba.horario(5, dueno, fecha);
        Cita cita = DatosPrueba.cita(ID_CITA, paciente, horario, estado);
        when(citaRepository.buscarConDetalle(ID_CITA)).thenReturn(Optional.of(cita));
        return cita;
    }

    private void conHistoria() {
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.of(historia));
    }

    private Atencion atencion(int idAtencion, int idCita, Doctor quien, LocalDate fecha, String diagnostico) {
        Atencion atencion = new Atencion();
        atencion.setIdAtencion(idAtencion);
        atencion.setHistoria(historia);
        atencion.setCita(DatosPrueba.cita(idCita, paciente, DatosPrueba.horario(idCita, quien, fecha), Cita.ATENDIDA));
        atencion.setDoctor(quien);
        atencion.setFechaAtencion(LocalDateTime.of(fecha, java.time.LocalTime.of(9, 0)));
        atencion.setDiagnostico(diagnostico);
        return atencion;
    }

    // ----- Agenda por rango de fechas -----

    @Test
    void agendaSinFechasTraeLasCitasDeHoy() {
        entra(doctor);
        Cita cita = DatosPrueba.cita(ID_CITA, paciente, DatosPrueba.horario(5, doctor, HOY), Cita.CONFIRMADA);
        when(citaRepository.delDoctorEntre(3, EN_AGENDA, HOY, HOY)).thenReturn(List.of(cita));

        List<CitaAsignadaResponse> agenda = servicio.agenda(sesion, null, null);

        assertEquals(1, agenda.size());
        assertEquals(ID_CITA, agenda.get(0).idCita());
        assertEquals(Cita.CONFIRMADA, agenda.get(0).estado());
    }

    @Test
    void agendaAceptaUnRangoDeHasta31Dias() {
        entra(doctor);
        when(citaRepository.delDoctorEntre(3, EN_AGENDA, HOY, HOY.plusDays(30))).thenReturn(List.of());

        assertEquals(List.of(), servicio.agenda(sesion, HOY, HOY.plusDays(30)));
    }

    @Test
    void agendaDeMasDe31DiasResponde400() {
        entra(doctor);

        ApiException error = exigeError(HttpStatus.BAD_REQUEST, () -> servicio.agenda(sesion, HOY, HOY.plusDays(31)));

        assertEquals("Elige un rango de hasta 31 días.", error.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    void agendaConFechasInvertidasResponde400() {
        entra(doctor);

        exigeError(HttpStatus.BAD_REQUEST, () -> servicio.agenda(sesion, HOY, HOY.minusDays(1)));

        verifyNoInteractions(citaRepository);
    }

    @Test
    void otroRolNoVeLaAgenda() {
        UsuarioSesion pacienteSesion = new UsuarioSesion(10, UsuarioSesion.PACIENTE);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.agenda(pacienteSesion, HOY, HOY));

        verifyNoInteractions(doctorRepository, citaRepository);
    }

    // ----- Consulta: paciente e historia clínica -----

    @Test
    void consultaReunePacienteAntecedentesEHistorial() {
        entra(doctor);
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        conHistoria();
        historia.setAlergias("Penicilina");
        Atencion propia = atencion(90, 50, doctor, HOY.minusDays(40), "Faringitis");
        Atencion deLaboratorio = atencion(91, 51, laboratorio, HOY.minusDays(12), "Hemograma normal");
        when(historialRepository.atenciones(ID_HISTORIA)).thenReturn(List.of(deLaboratorio, propia));

        DetalleMedicamento amoxicilina = new DetalleMedicamento();
        amoxicilina.setAtencion(propia);
        amoxicilina.setNombreMedicamento("Amoxicilina 500 mg");
        amoxicilina.setFrecuencia("Cada 8 horas");
        when(historialRepository.medicamentos(List.of(91, 90))).thenReturn(List.of(amoxicilina));

        ResultadoExamen hemoglobina = new ResultadoExamen();
        hemoglobina.setAtencion(deLaboratorio);
        hemoglobina.setTipoExamen("Hemoglobina");
        hemoglobina.setResultado("13.4 g/dL");
        when(resultadoRepository.deAtenciones(List.of(91, 90))).thenReturn(List.of(hemoglobina));

        DocumentoMedico receta = new DocumentoMedico();
        receta.setIdDocumento(12);
        receta.setAtencion(propia);
        receta.setTipoDocumento("Receta médica");
        receta.setNombreArchivo("receta.pdf");
        when(documentoRepository.delPaciente(1)).thenReturn(List.of(receta));
        when(reporteRepository.conDerivacion(List.of(91, 90))).thenReturn(List.of(90));

        ConsultaResponse consulta = servicio.consulta(sesion, ID_CITA);

        assertEquals("Medicina General", consulta.especialidad());
        assertEquals(Especialidad.CONSULTA, consulta.modo());
        assertTrue(consulta.puedeAtender());
        assertNull(consulta.idAtencion());
        assertEquals("Ana María Quispe Rojas", consulta.paciente().nombreCompleto());
        assertEquals(28, consulta.paciente().edad());
        assertEquals("HC-000001", consulta.paciente().numeroHistoria());
        assertEquals("Penicilina", consulta.paciente().alergias());

        assertEquals(2, consulta.historial().size());
        AtencionPreviaResponse examen = consulta.historial().get(0);
        assertEquals("Laboratorio", examen.especialidad());
        assertEquals(Especialidad.APOYO_DIAGNOSTICO, examen.tipoEspecialidad());
        assertFalse(examen.propia());
        assertFalse(examen.tieneDerivacion());
        assertEquals("Hemoglobina", examen.resultados().get(0).tipoExamen());
        assertEquals(List.of(), examen.medicamentos());
        AtencionPreviaResponse consultaPrevia = consulta.historial().get(1);
        assertTrue(consultaPrevia.propia());
        assertTrue(consultaPrevia.tieneDerivacion());
        assertEquals("Amoxicilina 500 mg", consultaPrevia.medicamentos().get(0).nombreMedicamento());
        assertEquals(12, consultaPrevia.documentos().get(0).idDocumento());
        assertEquals(List.of(), consultaPrevia.resultados());
    }

    @Test
    void consultaDeUnaCitaFuturaAunNoSePuedeAtender() {
        entra(doctor);
        citaDe(doctor, HOY.plusDays(2), Cita.CONFIRMADA);
        conHistoria();
        when(historialRepository.atenciones(ID_HISTORIA)).thenReturn(List.of());

        ConsultaResponse consulta = servicio.consulta(sesion, ID_CITA);

        assertFalse(consulta.puedeAtender());
        assertEquals(List.of(), consulta.historial());
        verifyNoInteractions(resultadoRepository, documentoRepository);
    }

    @Test
    void consultaDeUnaCitaAtendidaIndicaSuAtencion() {
        entra(doctor);
        citaDe(doctor, HOY, Cita.ATENDIDA);
        conHistoria();
        when(historialRepository.atenciones(ID_HISTORIA))
                .thenReturn(List.of(atencion(90, ID_CITA, doctor, HOY, "Faringitis")));
        when(historialRepository.medicamentos(List.of(90))).thenReturn(List.of());
        when(resultadoRepository.deAtenciones(List.of(90))).thenReturn(List.of());
        when(documentoRepository.delPaciente(1)).thenReturn(List.of());

        ConsultaResponse consulta = servicio.consulta(sesion, ID_CITA);

        assertEquals(90, consulta.idAtencion());
        assertFalse(consulta.puedeAtender());
    }

    @Test
    void consultaDeUnPacienteSinHistoriaClinicaNoFalla() {
        entra(doctor);
        citaDe(doctor, HOY, Cita.RESERVADA);
        when(historiaRepository.findByPaciente_IdPaciente(1)).thenReturn(Optional.empty());

        ConsultaResponse consulta = servicio.consulta(sesion, ID_CITA);

        assertNull(consulta.paciente().numeroHistoria());
        assertEquals(List.of(), consulta.historial());
        verifyNoInteractions(historialRepository);
    }

    @Test
    void consultaDeLaCitaDeOtroDoctorResponde404() {
        entra(doctor);
        citaDe(laboratorio, HOY, Cita.CONFIRMADA);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.consulta(sesion, ID_CITA));

        verifyNoInteractions(historiaRepository, historialRepository);
    }

    // ----- Antecedentes del paciente -----

    @Test
    void actualizarAntecedentesGuardaElTextoLimpio() {
        entra(doctor);
        citaDe(doctor, HOY, Cita.CONFIRMADA);
        conHistoria();
        historia.setMedicacionHabitual("Losartán");

        PacienteResponse respuesta = servicio.actualizarAntecedentes(sesion, ID_CITA,
                new AntecedentesRequest(" Penicilina ", "Asma leve en la infancia.", "   "));

        assertEquals("Penicilina", historia.getAlergias());
        assertEquals("Asma leve en la infancia.", historia.getAntecedentes());
        assertNull(historia.getMedicacionHabitual());
        assertEquals("Penicilina", respuesta.alergias());
    }

    @Test
    void antecedentesDeLaCitaDeOtroDoctorResponde404() {
        entra(doctor);
        citaDe(laboratorio, HOY, Cita.CONFIRMADA);

        exigeError(HttpStatus.NOT_FOUND,
                () -> servicio.actualizarAntecedentes(sesion, ID_CITA, new AntecedentesRequest("Polen", null, null)));

        assertNull(historia.getAlergias());
    }

    // ----- Reporte de exámenes -----

    private static ReporteExamenesRequest reporte() {
        return new ReporteExamenesRequest(List.of(
                new ResultadoExamenRequest(" Colesterol total ", " 182 mg/dL ", "Menor de 200 mg/dL", "Normal"),
                new ResultadoExamenRequest("Triglicéridos", "210 mg/dL", "  ", null)));
    }

    @Test
    void registrarResultadosLosGuardaEnOrdenYAvisaAlPaciente() {
        entra(laboratorio);
        Atencion atencion = atencion(91, ID_CITA, laboratorio, HOY, "Perfil lipídico");
        when(atencionRepository.findById(91)).thenReturn(Optional.of(atencion));
        when(resultadoRepository.existsByAtencion_IdAtencion(91)).thenReturn(false);

        List<ResultadoExamenResponse> respuesta = servicio.registrarResultados(sesion, 91, reporte());

        verify(resultadoRepository).saveAll(resultadosGuardados.capture());
        List<ResultadoExamen> guardados = resultadosGuardados.getValue();
        assertEquals(2, guardados.size());
        assertSame(atencion, guardados.get(0).getAtencion());
        assertEquals("Colesterol total", guardados.get(0).getTipoExamen());
        assertEquals("182 mg/dL", guardados.get(0).getResultado());
        assertEquals(1, guardados.get(0).getOrden());
        assertNull(guardados.get(1).getValorReferencia());
        assertEquals(2, guardados.get(1).getOrden());
        assertEquals("Triglicéridos", respuesta.get(1).tipoExamen());
        verify(notificacionService).crear(eq(paciente.getUsuario()), eq(Notificacion.SISTEMA),
                eq("Resultados de exámenes disponibles"), contains("2 exámenes"));
    }

    @Test
    void unaEspecialidadDeConsultaNoReportaResultados() {
        entra(doctor);

        exigeError(HttpStatus.FORBIDDEN, () -> servicio.registrarResultados(sesion, 91, reporte()));

        verifyNoInteractions(atencionRepository, resultadoRepository, notificacionService);
    }

    @Test
    void resultadosDeLaAtencionDeOtroProfesionalResponde404() {
        entra(laboratorio);
        when(atencionRepository.findById(91)).thenReturn(Optional.of(atencion(91, ID_CITA, doctor, HOY, "Consulta")));

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.registrarResultados(sesion, 91, reporte()));

        verifyNoInteractions(resultadoRepository, notificacionService);
    }

    @Test
    void resultadosRepetidosResponde409() {
        entra(laboratorio);
        when(atencionRepository.findById(91))
                .thenReturn(Optional.of(atencion(91, ID_CITA, laboratorio, HOY, "Perfil lipídico")));
        when(resultadoRepository.existsByAtencion_IdAtencion(91)).thenReturn(true);

        exigeError(HttpStatus.CONFLICT, () -> servicio.registrarResultados(sesion, 91, reporte()));

        verify(resultadoRepository, never()).saveAll(any());
        verifyNoInteractions(notificacionService);
    }

    // ----- Documentos del paciente -----

    private DocumentoMedico documentoDelPaciente() {
        DocumentoMedico documento = new DocumentoMedico();
        documento.setIdDocumento(12);
        documento.setAtencion(atencion(91, 51, laboratorio, HOY.minusDays(12), "Hemograma normal"));
        when(documentoRepository.findById(12)).thenReturn(Optional.of(documento));
        return documento;
    }

    @Test
    void elDoctorDescargaLosDocumentosDeSuPaciente() {
        entra(doctor);
        DocumentoMedico documento = documentoDelPaciente();
        CitaService.Archivo archivo = new CitaService.Archivo("hemograma.pdf", "application/pdf", new byte[] {1});
        when(citaRepository.doctorAtiendeA(3, 1)).thenReturn(true);
        when(citaService.archivoDe(documento)).thenReturn(archivo);

        assertSame(archivo, servicio.documento(sesion, 12));
    }

    @Test
    void sinCitaConElPacienteElDocumentoResponde404() {
        entra(doctor);
        documentoDelPaciente();
        when(citaRepository.doctorAtiendeA(3, 1)).thenReturn(false);

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.documento(sesion, 12));

        verifyNoInteractions(citaService);
    }

    @Test
    void documentoInexistenteResponde404() {
        entra(doctor);
        when(documentoRepository.findById(12)).thenReturn(Optional.empty());

        exigeError(HttpStatus.NOT_FOUND, () -> servicio.documento(sesion, 12));

        verifyNoInteractions(citaService);
    }
}
