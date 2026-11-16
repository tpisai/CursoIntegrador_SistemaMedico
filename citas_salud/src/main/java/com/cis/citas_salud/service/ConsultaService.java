package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.AntecedentesRequest;
import com.cis.citas_salud.api.dto.ConsultaDto.AtencionPreviaResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.ConsultaResponse;
import com.cis.citas_salud.api.dto.ConsultaDto.MedicamentoResponse;
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
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.ResultadoExamen;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.HistorialClinicoRepository;
import com.cis.citas_salud.repository.ReporteClinicoRepository;
import com.cis.citas_salud.repository.ResultadoExamenRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Página de consulta del doctor: agenda por rango de fechas, historia clínica del paciente con sus
 * documentos y resultados de exámenes de los servicios de apoyo al diagnóstico.
 */
@Service
@Transactional
public class ConsultaService {

    private static final int MAX_DIAS_AGENDA = 31;
    // La agenda muestra lo pendiente y lo ya cerrado; las citas canceladas no ocupan turno.
    private static final List<String> EN_AGENDA =
            List.of(Cita.RESERVADA, Cita.CONFIRMADA, Cita.ATENDIDA, Cita.NO_ASISTIO);

    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final HistorialClinicoRepository historialRepository;
    private final AtencionRepository atencionRepository;
    private final DocumentoMedicoRepository documentoRepository;
    private final ResultadoExamenRepository resultadoRepository;
    private final ReporteClinicoRepository reporteRepository;
    private final NotificacionService notificacionService;
    private final CitaService citaService;

    public ConsultaService(DoctorRepository doctorRepository, CitaRepository citaRepository,
                           HistoriaClinicaRepository historiaRepository, HistorialClinicoRepository historialRepository,
                           AtencionRepository atencionRepository, DocumentoMedicoRepository documentoRepository,
                           ResultadoExamenRepository resultadoRepository, ReporteClinicoRepository reporteRepository,
                           NotificacionService notificacionService, CitaService citaService) {
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.historiaRepository = historiaRepository;
        this.historialRepository = historialRepository;
        this.atencionRepository = atencionRepository;
        this.documentoRepository = documentoRepository;
        this.resultadoRepository = resultadoRepository;
        this.reporteRepository = reporteRepository;
        this.notificacionService = notificacionService;
        this.citaService = citaService;
    }

    /** Citas del doctor entre dos fechas. Sin fechas, las de hoy. */
    @Transactional(readOnly = true)
    public List<CitaAsignadaResponse> agenda(UsuarioSesion sesion, LocalDate desde, LocalDate hasta) {
        Doctor doctor = doctor(sesion);
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio;
        if (fin.isBefore(inicio)) {
            throw ApiException.datosInvalidos("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) >= MAX_DIAS_AGENDA) {
            throw ApiException.datosInvalidos("Elige un rango de hasta " + MAX_DIAS_AGENDA + " días.");
        }
        return citaRepository.delDoctorEntre(doctor.getIdDoctor(), EN_AGENDA, inicio, fin).stream()
                .map(DoctorService::respuesta).toList();
    }

    /** Todo lo que el doctor ve al atender una cita suya: paciente, antecedentes e historia clínica. */
    @Transactional(readOnly = true)
    public ConsultaResponse consulta(UsuarioSesion sesion, Integer idCita) {
        Doctor doctor = doctor(sesion);
        Cita cita = citaDelDoctor(doctor, idCita);
        Paciente paciente = cita.getPaciente();
        HistoriaClinica historia = historiaRepository.findByPaciente_IdPaciente(paciente.getIdPaciente()).orElse(null);
        List<AtencionPreviaResponse> historial = historia == null ? List.of() : historial(historia, paciente, doctor);

        Integer idAtencion = historial.stream()
                .filter(a -> a.idCita().equals(idCita))
                .map(AtencionPreviaResponse::idAtencion)
                .findFirst().orElse(null);
        boolean puedeAtender = cita.estaActiva() && !cita.getHorario().getFecha().isAfter(LocalDate.now());
        Especialidad especialidad = doctor.getEspecialidad();
        return new ConsultaResponse(DoctorService.respuesta(cita), especialidad.getNombre(), especialidad.getTipo(),
                puedeAtender, idAtencion, respuesta(paciente, historia), historial);
    }

    /** El doctor actualiza las alergias, los antecedentes y la medicación habitual del paciente. */
    public PacienteResponse actualizarAntecedentes(UsuarioSesion sesion, Integer idCita, AntecedentesRequest datos) {
        Cita cita = citaDelDoctor(doctor(sesion), idCita);
        Paciente paciente = cita.getPaciente();
        HistoriaClinica historia = historiaRepository.findByPaciente_IdPaciente(paciente.getIdPaciente())
                .orElseThrow(() -> ApiException.conflicto("El paciente no tiene historia clínica abierta."));
        historia.setAlergias(textoOpcional(datos.alergias()));
        historia.setAntecedentes(textoOpcional(datos.antecedentes()));
        historia.setMedicacionHabitual(textoOpcional(datos.medicacionHabitual()));
        return respuesta(paciente, historia);
    }

    /** Reporte de exámenes: lo registra un servicio de apoyo al diagnóstico sobre una atención suya. */
    public List<ResultadoExamenResponse> registrarResultados(UsuarioSesion sesion, Integer idAtencion,
                                                             ReporteExamenesRequest datos) {
        Doctor doctor = doctor(sesion);
        if (!doctor.getEspecialidad().esApoyoDiagnostico()) {
            throw ApiException.prohibido("Solo los servicios de apoyo al diagnóstico reportan resultados de exámenes.");
        }
        Atencion atencion = atencionRepository.findById(idAtencion)
                .filter(a -> a.getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa atención."));
        if (resultadoRepository.existsByAtencion_IdAtencion(idAtencion)) {
            throw ApiException.conflicto("Esta atención ya tiene resultados registrados.");
        }

        List<ResultadoExamen> resultados = new ArrayList<>();
        for (ResultadoExamenRequest dato : datos.resultados()) {
            ResultadoExamen resultado = new ResultadoExamen();
            resultado.setAtencion(atencion);
            resultado.setTipoExamen(dato.tipoExamen().trim());
            resultado.setResultado(dato.resultado().trim());
            resultado.setValorReferencia(textoOpcional(dato.valorReferencia()));
            resultado.setInterpretacion(textoOpcional(dato.interpretacion()));
            resultado.setOrden(resultados.size() + 1);
            resultados.add(resultado);
        }
        resultadoRepository.saveAll(resultados);

        String cuantos = resultados.size() == 1 ? "1 examen" : resultados.size() + " exámenes";
        notificacionService.crear(atencion.getHistoria().getPaciente().getUsuario(), Notificacion.SISTEMA,
                "Resultados de exámenes disponibles",
                doctor.getEspecialidad().getNombre() + " registró los resultados de " + cuantos
                        + ". Tu médico los verá en tu próxima consulta.");
        return resultados.stream().map(ConsultaService::respuesta).toList();
    }

    /** El doctor descarga un documento de un paciente con el que tiene o tuvo una cita. */
    @Transactional(readOnly = true)
    public Archivo documento(UsuarioSesion sesion, Integer idDocumento) {
        Doctor doctor = doctor(sesion);
        DocumentoMedico documento = documentoRepository.findById(idDocumento)
                .filter(d -> citaRepository.doctorAtiendeA(doctor.getIdDoctor(),
                        d.getAtencion().getHistoria().getPaciente().getIdPaciente()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese documento."));
        return citaService.archivoDe(documento);
    }

    private List<AtencionPreviaResponse> historial(HistoriaClinica historia, Paciente paciente, Doctor doctor) {
        List<Atencion> atenciones = historialRepository.atenciones(historia.getIdHistoria());
        if (atenciones.isEmpty()) return List.of();
        List<Integer> ids = atenciones.stream().map(Atencion::getIdAtencion).toList();

        Map<Integer, List<MedicamentoResponse>> medicamentos = historialRepository.medicamentos(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getAtencion().getIdAtencion(),
                        Collectors.mapping(ConsultaService::respuesta, Collectors.toList())));
        Map<Integer, List<ResultadoExamenResponse>> resultados = resultadoRepository.deAtenciones(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getAtencion().getIdAtencion(),
                        Collectors.mapping(ConsultaService::respuesta, Collectors.toList())));
        Map<Integer, List<DocumentoResponse>> documentos = documentoRepository.delPaciente(paciente.getIdPaciente()).stream()
                .collect(Collectors.groupingBy(d -> d.getAtencion().getIdAtencion(),
                        Collectors.mapping(ConsultaService::respuesta, Collectors.toList())));
        Set<Integer> conDerivacion = Set.copyOf(reporteRepository.conDerivacion(ids));

        return atenciones.stream()
                .map(a -> new AtencionPreviaResponse(
                        a.getIdAtencion(),
                        a.getCita().getIdCita(),
                        Formato.fecha(a.getFechaAtencion().toLocalDate()),
                        a.getDoctor().getEspecialidad().getNombre(),
                        a.getDoctor().getEspecialidad().getTipo(),
                        a.getDoctor().getUsuario().getNombreCorto(),
                        a.getDoctor().getIdDoctor().equals(doctor.getIdDoctor()),
                        conDerivacion.contains(a.getIdAtencion()),
                        a.getAnamnesis(),
                        a.getExamenFisico(),
                        a.getDiagnostico(),
                        a.getTratamiento(),
                        a.getObservaciones(),
                        medicamentos.getOrDefault(a.getIdAtencion(), List.of()),
                        resultados.getOrDefault(a.getIdAtencion(), List.of()),
                        documentos.getOrDefault(a.getIdAtencion(), List.of())))
                .toList();
    }

    private Cita citaDelDoctor(Doctor doctor, Integer idCita) {
        return citaRepository.buscarConDetalle(idCita)
                .filter(c -> c.getHorario().getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita en tu agenda."));
    }

    private Doctor doctor(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);
        return doctorRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de doctor."));
    }

    private static PacienteResponse respuesta(Paciente paciente, HistoriaClinica historia) {
        Usuario usuario = paciente.getUsuario();
        LocalDate nacimiento = paciente.getFechaNacimiento();
        return new PacienteResponse(
                paciente.getIdPaciente(),
                usuario.getNombres() + " " + usuario.getApellidos(),
                usuario.getDni(),
                nacimiento == null ? null : Formato.fecha(nacimiento),
                nacimiento == null ? null : Period.between(nacimiento, LocalDate.now()).getYears(),
                paciente.getSexo(),
                paciente.getGrupoSanguineo(),
                usuario.getTelefono(),
                usuario.getCorreo(),
                paciente.getDireccion(),
                paciente.getContactoEmergencia(),
                paciente.getTelefonoEmergencia(),
                historia == null ? null : historia.getNumeroHistoria(),
                historia == null ? null : historia.getAlergias(),
                historia == null ? null : historia.getAntecedentes(),
                historia == null ? null : historia.getMedicacionHabitual());
    }

    private static MedicamentoResponse respuesta(DetalleMedicamento m) {
        return new MedicamentoResponse(m.getNombreMedicamento(), m.getDosis(), m.getPresentacion(),
                m.getViaAdministracion(), m.getUnidadesPorToma(), m.getFrecuencia(), m.getDuracion(), m.getCantidad());
    }

    private static ResultadoExamenResponse respuesta(ResultadoExamen r) {
        return new ResultadoExamenResponse(r.getTipoExamen(), r.getResultado(), r.getValorReferencia(),
                r.getInterpretacion());
    }

    private static DocumentoResponse respuesta(DocumentoMedico d) {
        return new DocumentoResponse(d.getIdDocumento(), d.getTipoDocumento(), d.getNombreArchivo(),
                Formato.fecha(d.getFechaCarga().toLocalDate()));
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
