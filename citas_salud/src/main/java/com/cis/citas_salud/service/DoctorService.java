package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.DoctorDto.AtencionResponse;
import com.cis.citas_salud.api.dto.DoctorDto.CambioFechaRequest;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import com.cis.citas_salud.api.dto.DoctorDto.DerivacionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.HorarioLibreResponse;
import com.cis.citas_salud.api.dto.DoctorDto.RegistrarAtencionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.SolicitudResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.DocumentoMedico;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Panel del doctor: citas asignadas (RF-14), cambio de fecha (RF-15) y derivación (RF-16). */
@Service
@Transactional
public class DoctorService {

    private static final int DIAS_PROPUESTA = 21;
    // Tipos de documento médico que el doctor puede adjuntar (RF-10).
    public static final List<String> TIPOS_DOCUMENTO = List.of(
            "Receta médica", "Resultados de laboratorio", "Diagnóstico del paciente", "Informe médico", "Otro");
    private static final List<String> TIPOS_ARCHIVO = List.of("application/pdf", "image/jpeg", "image/png");

    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtencionRepository atencionRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final DocumentoMedicoRepository documentoRepository;
    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;
    private final Path carpetaArchivos;

    public DoctorService(DoctorRepository doctorRepository, CitaRepository citaRepository,
                         HorarioRepository horarioRepository, SolicitudRepository solicitudRepository,
                         UsuarioRepository usuarioRepository, AtencionRepository atencionRepository,
                         HistoriaClinicaRepository historiaRepository, DocumentoMedicoRepository documentoRepository,
                         NotificacionService notificacionService, ObjectMapper objectMapper,
                         @Value("${saludgrau.archivos.carpeta:archivos}") String carpetaArchivos) {
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.atencionRepository = atencionRepository;
        this.historiaRepository = historiaRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
        this.carpetaArchivos = Path.of(carpetaArchivos).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<CitaAsignadaResponse> citasDelDia(UsuarioSesion sesion, LocalDate fecha) {
        return citaRepository.delDoctorEntre(doctor(sesion).getIdDoctor(), Cita.ACTIVAS, fecha, fecha).stream()
                .map(DoctorService::respuesta).toList();
    }

    /** Citas activas desde hoy: sirven para elegir la cita a reprogramar o el paciente a derivar. */
    @Transactional(readOnly = true)
    public List<CitaAsignadaResponse> citasProximas(UsuarioSesion sesion) {
        LocalDate hoy = LocalDate.now();
        return citaRepository.delDoctorEntre(doctor(sesion).getIdDoctor(), Cita.ACTIVAS, hoy, hoy.plusYears(1)).stream()
                .map(DoctorService::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioLibreResponse> horariosLibres(UsuarioSesion sesion) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        return horarioRepository.findByDoctor_IdDoctorAndFechaBetweenAndEstadoOrderByFechaAscHoraInicioAsc(
                        doctor(sesion).getIdDoctor(), hoy, hoy.plusDays(DIAS_PROPUESTA), Horario.DISPONIBLE).stream()
                .filter(h -> h.estaLibre(ahora))
                .map(h -> new HorarioLibreResponse(h.getIdHorario(), Formato.fecha(h.getFecha()),
                        Formato.hora(h.getHoraInicio()), h.getConsultorio().getUbicacion()))
                .toList();
    }

    public SolicitudResponse solicitarCambioFecha(UsuarioSesion sesion, CambioFechaRequest datos) {
        Doctor doctor = doctor(sesion);
        Cita cita = citaActivaDelDoctor(doctor, datos.idCita());
        Horario nuevo = horarioRepository.findById(datos.idHorarioNuevo())
                .filter(h -> h.getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese horario en tu agenda."));
        if (!nuevo.estaLibre(LocalDateTime.now())) {
            throw ApiException.conflicto("El horario propuesto ya no está libre. Elige otro.");
        }
        exigirSinPendiente(cita, Solicitud.CAMBIO_HORARIO, "Ya enviaste un cambio de fecha para esta cita y sigue pendiente.");

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("idHorarioNuevo", nuevo.getIdHorario());
        detalle.put("fecha", Formato.fecha(nuevo.getFecha()));
        detalle.put("horaInicio", Formato.hora(nuevo.getHoraInicio()));
        detalle.put("consultorio", nuevo.getConsultorio().getUbicacion());
        Solicitud solicitud = guardar(doctor, cita, Solicitud.CAMBIO_HORARIO, datos.motivo(), detalle);

        avisar(doctor, "Solicitud de cambio de fecha enviada",
                "Propusiste mover la cita de " + Formato.nombrePaciente(cita.getPaciente().getUsuario()) + " al "
                        + Formato.diaHora(nuevo.getFecha(), nuevo.getHoraInicio()) + ". Te avisaremos cuando administración responda.");
        return respuesta(solicitud);
    }

    public SolicitudResponse derivar(UsuarioSesion sesion, DerivacionRequest datos) {
        Doctor doctor = doctor(sesion);
        Cita cita = citaActivaDelDoctor(doctor, datos.idCita());
        exigirSinPendiente(cita, Solicitud.DERIVACION, "Ya enviaste una derivación para este paciente y sigue pendiente.");

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("establecimiento", datos.establecimiento().trim());
        detalle.put("especialidad", datos.especialidad().trim());
        Solicitud solicitud = guardar(doctor, cita, Solicitud.DERIVACION, datos.motivo(), detalle);

        avisar(doctor, "Derivación enviada a administración",
                "Solicitaste derivar a " + Formato.nombrePaciente(cita.getPaciente().getUsuario()) + " a "
                        + datos.establecimiento().trim() + " (" + datos.especialidad().trim() + ").");
        return respuesta(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> solicitudes(UsuarioSesion sesion) {
        return solicitudRepository.delDoctor(doctor(sesion).getIdDoctor()).stream().map(this::respuesta).toList();
    }

    /** RF-10: el doctor registra la atención (diagnóstico, tratamiento) o la inasistencia del paciente. */
    public AtencionResponse registrarAtencion(UsuarioSesion sesion, Integer idCita, RegistrarAtencionRequest datos) {
        Doctor doctor = doctor(sesion);
        Cita cita = citaActivaDelDoctor(doctor, idCita);
        Horario horario = cita.getHorario();
        if (horario.getFecha().isAfter(LocalDate.now())) {
            throw ApiException.conflicto("Solo puedes registrar la atención de citas de hoy o de días anteriores.");
        }
        if (atencionRepository.existsByCita_IdCita(idCita)) {
            throw ApiException.conflicto("Esta cita ya tiene una atención registrada.");
        }

        horario.setEstado(Horario.FINALIZADO);
        if (!datos.asistio()) {
            cita.setEstado(Cita.NO_ASISTIO);
            return new AtencionResponse(idCita, Cita.NO_ASISTIO, null);
        }

        String diagnostico = textoOpcional(datos.diagnostico());
        String tratamiento = textoOpcional(datos.tratamiento());
        if (diagnostico == null) throw ApiException.datosInvalidos("Escribe el diagnóstico.");
        if (tratamiento == null) throw ApiException.datosInvalidos("Escribe el tratamiento indicado.");

        Atencion atencion = new Atencion();
        atencion.setHistoria(historiaRepository.findByPaciente_IdPaciente(cita.getPaciente().getIdPaciente())
                .orElseThrow(() -> ApiException.conflicto("El paciente no tiene historia clínica abierta.")));
        atencion.setCita(cita);
        atencion.setDoctor(doctor);
        atencion.setEstado("FINALIZADA");
        atencion.setAnamnesis(textoOpcional(datos.anamnesis()));
        atencion.setExamenFisico(textoOpcional(datos.examenFisico()));
        atencion.setDiagnostico(diagnostico);
        atencion.setTratamiento(tratamiento);
        atencion.setObservaciones(textoOpcional(datos.observaciones()));
        atencionRepository.save(atencion);
        cita.setEstado(Cita.ATENDIDA);

        notificacionService.crear(cita.getPaciente().getUsuario(), Notificacion.CITA, "Atención registrada",
                doctor.getUsuario().getNombreCorto() + " registró tu atención del "
                        + Formato.diaHora(horario.getFecha(), horario.getHoraInicio())
                        + ". Puedes verla en tu historial médico.");
        return new AtencionResponse(idCita, Cita.ATENDIDA, atencion.getIdAtencion());
    }

    /** RF-10: adjunta un archivo (receta, resultados…) a una atención; el paciente lo descarga (RF-11). */
    public DocumentoResponse subirDocumento(UsuarioSesion sesion, Integer idAtencion, String tipoDocumento,
                                            MultipartFile archivo) {
        Doctor doctor = doctor(sesion);
        Atencion atencion = atencionRepository.findById(idAtencion)
                .filter(a -> a.getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa atención."));
        if (tipoDocumento == null || !TIPOS_DOCUMENTO.contains(tipoDocumento)) {
            throw ApiException.datosInvalidos("Elige el tipo de documento.");
        }
        if (archivo == null || archivo.isEmpty()) throw ApiException.datosInvalidos("Adjunta un archivo.");
        if (!TIPOS_ARCHIVO.contains(archivo.getContentType())) {
            throw ApiException.datosInvalidos("Solo se aceptan archivos PDF, JPG o PNG.");
        }

        String nombre = nombreSeguro(archivo.getOriginalFilename());
        String ruta = "atenciones/" + idAtencion + "/" + System.currentTimeMillis() + "-" + nombre;
        try {
            Path destino = carpetaArchivos.resolve(ruta).normalize();
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }

        DocumentoMedico documento = new DocumentoMedico();
        documento.setAtencion(atencion);
        documento.setTipoDocumento(tipoDocumento);
        documento.setNombreArchivo(nombre);
        documento.setRutaArchivo(ruta);
        documento.setTipoMime(archivo.getContentType());
        documentoRepository.save(documento);

        notificacionService.crear(atencion.getHistoria().getPaciente().getUsuario(), Notificacion.SISTEMA,
                "Nuevo documento disponible", tipoDocumento + " ya está en tu historial médico para descargar.");
        return new DocumentoResponse(documento.getIdDocumento(), tipoDocumento, nombre,
                Formato.fecha(documento.getFechaCarga().toLocalDate()));
    }

    /** Deja solo letras, números, punto, guion y guion bajo (evita rutas como "../"). */
    private static String nombreSeguro(String original) {
        String base = original == null ? "documento" : Path.of(original).getFileName().toString();
        String limpio = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (limpio.isBlank() || limpio.startsWith(".")) limpio = "documento" + limpio;
        return limpio.length() > 100 ? limpio.substring(limpio.length() - 100) : limpio;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Solicitud guardar(Doctor doctor, Cita cita, String tipo, String motivo, Map<String, Object> detalle) {
        Solicitud solicitud = new Solicitud();
        solicitud.setDoctor(doctor);
        solicitud.setCita(cita);
        solicitud.setTipo(tipo);
        solicitud.setMotivo(motivo.trim());
        solicitud.setDatosAdicionales(objectMapper.writeValueAsString(detalle));
        return solicitudRepository.save(solicitud);
    }

    /** Confirma al doctor y avisa al personal de administración que debe revisar (RF-17, RF-18). */
    private void avisar(Doctor doctor, String titulo, String mensaje) {
        notificacionService.crear(doctor.getUsuario(), Notificacion.SOLICITUD, titulo, mensaje);
        usuarioRepository.findByRol_NombreAndEstado("ADMINISTRADOR", "ACTIVO").forEach(admin ->
                notificacionService.crear(admin, Notificacion.SOLICITUD, "Nueva solicitud por revisar",
                        doctor.getUsuario().getNombreCorto() + ": " + mensaje));
    }

    private void exigirSinPendiente(Cita cita, String tipo, String mensaje) {
        if (solicitudRepository.existsByCita_IdCitaAndTipoAndEstado(cita.getIdCita(), tipo, Solicitud.PENDIENTE)) {
            throw ApiException.conflicto(mensaje);
        }
    }

    private Cita citaActivaDelDoctor(Doctor doctor, Integer idCita) {
        Cita cita = citaRepository.buscarConDetalle(idCita)
                .filter(c -> c.getHorario().getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita en tu agenda."));
        if (!cita.estaActiva()) throw ApiException.conflicto("Esa cita ya no está activa.");
        return cita;
    }

    private Doctor doctor(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);
        return doctorRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de doctor."));
    }

    private static CitaAsignadaResponse respuesta(Cita c) {
        Horario h = c.getHorario();
        return new CitaAsignadaResponse(
                c.getIdCita(),
                c.getNumeroTicket(),
                c.getEstado(),
                Formato.fecha(h.getFecha()),
                Formato.hora(h.getHoraInicio()),
                Formato.hora(h.getHoraFin()),
                Formato.nombrePaciente(c.getPaciente().getUsuario()),
                c.getPaciente().getUsuario().getDni(),
                c.getMotivo(),
                h.getConsultorio().getUbicacion());
    }

    private SolicitudResponse respuesta(Solicitud s) {
        Horario h = s.getCita().getHorario();
        return new SolicitudResponse(
                s.getIdSolicitud(),
                s.getTipo(),
                s.getEstado(),
                Formato.fechaHora(s.getFechaSolicitud()),
                Formato.nombrePaciente(s.getCita().getPaciente().getUsuario()),
                Formato.fecha(h.getFecha()),
                Formato.hora(h.getHoraInicio()),
                detalle(s),
                s.getMotivo(),
                s.getRespuestaAdmin());
    }

    private String detalle(Solicitud s) {
        return detalleLegible(s, objectMapper);
    }

    /** "Nueva fecha: Jue 1 oct · 09:30" o "Hospital … · Cardiología" (también lo usa el panel de administración). */
    static String detalleLegible(Solicitud s, ObjectMapper objectMapper) {
        if (s.getDatosAdicionales() == null) return null;
        Map<?, ?> datos = objectMapper.readValue(s.getDatosAdicionales(), Map.class);
        if (Solicitud.CAMBIO_HORARIO.equals(s.getTipo())) {
            LocalDate fecha = LocalDate.parse(String.valueOf(datos.get("fecha")));
            return "Nueva fecha: " + Formato.diaHora(fecha, LocalTime.parse(String.valueOf(datos.get("horaInicio"))));
        }
        return datos.get("establecimiento") + " · " + datos.get("especialidad");
    }
}
