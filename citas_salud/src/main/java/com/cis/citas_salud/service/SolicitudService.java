package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.SolicitudDto.CambioPacienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.CambioPendienteResponse;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolverCambioRequest;
import com.cis.citas_salud.api.dto.SolicitudDto.ResolucionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cambios de fecha vistos desde el portal del paciente (RF-17): la cita aprobada por administración
 * espera su decisión — nuevo horario con el mismo doctor, cita con otro doctor o cancelación — y la
 * constancia en PDF que descargan el paciente, el doctor y administración.
 */
@Service
@Transactional
public class SolicitudService {

    /** Plantilla en {@code src/main/resources/reports/ConstanciaCambioFecha.jrxml}. */
    private static final String CONSTANCIA = "ConstanciaCambioFecha";

    private final SolicitudRepository solicitudRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaService citaService;
    private final NotificacionService notificacionService;
    private final GeneradorReportesSql generador;
    private final ObjectMapper objectMapper;

    public SolicitudService(SolicitudRepository solicitudRepository, CitaRepository citaRepository,
                            HorarioRepository horarioRepository, PacienteRepository pacienteRepository,
                            CitaService citaService, NotificacionService notificacionService,
                            GeneradorReportesSql generador, ObjectMapper objectMapper) {
        this.solicitudRepository = solicitudRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.citaService = citaService;
        this.notificacionService = notificacionService;
        this.generador = generador;
        this.objectMapper = objectMapper;
    }

    /** Cambios de fecha aprobados que el paciente debe resolver antes de seguir usando su portal. */
    @Transactional(readOnly = true)
    public List<CambioPendienteResponse> pendientes(UsuarioSesion sesion) {
        Paciente paciente = paciente(sesion);
        LocalDateTime ahora = LocalDateTime.now();
        return solicitudRepository.cambiosAprobadosDelPaciente(paciente.getIdPaciente()).stream()
                .filter(s -> s.getCita().estaActiva() && !s.getCita().getHorario().yaPaso(ahora))
                .map(this::pendiente)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CambioPacienteResponse> mias(UsuarioSesion sesion) {
        Paciente paciente = paciente(sesion);
        return solicitudRepository.cambiosDelPaciente(paciente.getIdPaciente()).stream()
                .map(this::fila)
                .toList();
    }

    /** El paciente elige qué hacer con la cita que administración aprobó para cambio de fecha. */
    public ResolucionResponse resolver(UsuarioSesion sesion, Integer idSolicitud, ResolverCambioRequest peticion) {
        Paciente paciente = paciente(sesion);
        // Primero la solicitud y después los horarios, siempre en ese orden (evita bloqueos cruzados).
        Solicitud solicitud = solicitudRepository.bloquear(idSolicitud)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa solicitud."));
        if (!Solicitud.CAMBIO_HORARIO.equals(solicitud.getTipo())) {
            throw ApiException.noEncontrado("No encontramos esa solicitud.");
        }
        Cita cita = citaRepository.buscarConDetalle(solicitud.getCita().getIdCita())
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita."));
        if (!cita.getPaciente().getIdPaciente().equals(paciente.getIdPaciente())) {
            throw ApiException.noEncontrado("No encontramos esa solicitud.");
        }
        if (!Solicitud.APROBADA.equals(solicitud.getEstado())) {
            throw ApiException.conflicto("Este cambio de fecha ya fue resuelto.");
        }
        if (!cita.estaActiva()) {
            throw ApiException.conflicto("Tu cita ya no está activa (fue atendida o cancelada).");
        }

        Map<String, Object> detalle = datosDe(solicitud);
        LocalDateTime ahora = LocalDateTime.now();
        Horario anterior = cita.getHorario();
        ResolucionResponse respuesta = switch (peticion.decision()) {
            case MISMO_DOCTOR -> moverConElMismoDoctor(cita, anterior, peticion.idHorarioNuevo(), detalle, ahora);
            case OTRO_DOCTOR -> reservarConOtroDoctor(paciente, cita, anterior, peticion.idHorarioNuevo(), detalle, ahora);
            case CANCELADA -> cancelarCita(cita, anterior, detalle, ahora);
        };

        detalle.put("decisionPaciente", peticion.decision().name());
        detalle.put("fechaResolucion", Formato.fechaHora(ahora));
        solicitud.setDatosAdicionales(objectMapper.writeValueAsString(detalle));
        solicitud.setEstado(Solicitud.RESUELTA);
        return respuesta;
    }

    /** Constancia en PDF de un cambio de fecha aprobado. La pueden descargar paciente, doctor y administración. */
    @Transactional(readOnly = true)
    public Archivo constancia(UsuarioSesion sesion, Integer idSolicitud) {
        Solicitud solicitud = solicitudRepository.buscarConDetalle(idSolicitud)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa solicitud."));
        if (!Solicitud.CAMBIO_HORARIO.equals(solicitud.getTipo())) {
            throw ApiException.noEncontrado("No encontramos esa solicitud.");
        }
        if (!Solicitud.APROBADA.equals(solicitud.getEstado()) && !Solicitud.RESUELTA.equals(solicitud.getEstado())) {
            throw ApiException.conflicto("La constancia está disponible cuando administración aprueba la solicitud.");
        }
        if (!puedeVer(sesion, solicitud)) {
            throw ApiException.noEncontrado("No encontramos esa solicitud.");
        }
        byte[] pdf = generador.pdf(CONSTANCIA, Map.of("ID_SOLICITUD", idSolicitud));
        return new Archivo("constancia-cambio-fecha-" + idSolicitud + ".pdf", "application/pdf", pdf);
    }

    // ----- Decisiones del paciente -----

    private ResolucionResponse moverConElMismoDoctor(Cita cita, Horario anterior, Integer idHorarioNuevo,
                                                     Map<String, Object> detalle, LocalDateTime ahora) {
        Horario nuevo = turnoElegido(anterior, idHorarioNuevo);
        if (!nuevo.getDoctor().getIdDoctor().equals(anterior.getDoctor().getIdDoctor())) {
            throw ApiException.datosInvalidos("Elige un horario con el mismo doctor.");
        }
        if (!nuevo.estaLibre(ahora)) {
            throw ApiException.conflicto("Ese horario acaba de ser tomado. Elige otro.");
        }

        // Reprogramar: el turno anterior se libera y la cita pasa al nuevo.
        anterior.setEstado(anterior.yaPaso(ahora) ? Horario.FINALIZADO : Horario.DISPONIBLE);
        nuevo.setEstado(Horario.RESERVADO);
        cita.setHorario(nuevo);

        String cuando = Formato.diaHora(nuevo.getFecha(), nuevo.getHoraInicio());
        detalle.put("nuevaFecha", Formato.fecha(nuevo.getFecha()));
        detalle.put("nuevaHora", Formato.hora(nuevo.getHoraInicio()));
        detalle.put("nuevoConsultorio", nuevo.getConsultorio().getUbicacion());
        detalle.put("nuevoDoctor", nuevo.getDoctor().getUsuario().getNombreCorto());
        detalle.put("nuevaEspecialidad", nuevo.getDoctor().getEspecialidad().getNombre());

        notificacionService.crear(anterior.getDoctor().getUsuario(), Notificacion.SOLICITUD,
                "El paciente cambió su cita",
                Formato.nombrePaciente(cita.getPaciente().getUsuario()) + " movió su cita del "
                        + Formato.diaHora(anterior.getFecha(), anterior.getHoraInicio()) + " al " + cuando + ".");
        return new ResolucionResponse(cita.getIdCita(), Solicitud.RESUELTA, Solicitud.MISMO_DOCTOR,
                "Tu cita quedó reprogramada para el " + cuando + " con "
                        + nuevo.getDoctor().getUsuario().getNombreCorto() + " en "
                        + nuevo.getConsultorio().getUbicacion() + ".",
                null, cita.getNumeroTicket());
    }

    private ResolucionResponse reservarConOtroDoctor(Paciente paciente, Cita cita, Horario anterior,
                                                     Integer idHorarioNuevo, Map<String, Object> detalle,
                                                     LocalDateTime ahora) {
        Horario nuevo = turnoElegido(anterior, idHorarioNuevo);
        if (nuevo.getDoctor().getIdDoctor().equals(anterior.getDoctor().getIdDoctor())) {
            throw ApiException.datosInvalidos("Elige un doctor distinto al actual.");
        }
        if (!nuevo.getDoctor().getEspecialidad().getIdEspecialidad()
                .equals(anterior.getDoctor().getEspecialidad().getIdEspecialidad())) {
            throw ApiException.datosInvalidos("Elige una cita de la misma especialidad.");
        }
        if (!nuevo.estaLibre(ahora)) {
            throw ApiException.conflicto("Ese horario acaba de ser tomado. Elige otro.");
        }

        // La cita original se cancela (y libera su turno) y se reserva una cita nueva con el otro doctor.
        citaService.cancelarCita(cita);
        nuevo.setEstado(Horario.RESERVADO);
        Cita nueva = new Cita();
        nueva.setPaciente(paciente);
        nueva.setHorario(nuevo);
        nueva.setEstado(Cita.RESERVADA);
        nueva.setMotivo(cita.getMotivo());
        nueva.setNumeroTicket(Formato.ticket(citaRepository.siguienteTicket()));
        citaRepository.save(nueva);

        String cuando = Formato.diaHora(nuevo.getFecha(), nuevo.getHoraInicio());
        String doctor = nuevo.getDoctor().getUsuario().getNombreCorto();
        detalle.put("nuevaFecha", Formato.fecha(nuevo.getFecha()));
        detalle.put("nuevaHora", Formato.hora(nuevo.getHoraInicio()));
        detalle.put("nuevoConsultorio", nuevo.getConsultorio().getUbicacion());
        detalle.put("nuevoDoctor", doctor);
        detalle.put("nuevaEspecialidad", nuevo.getDoctor().getEspecialidad().getNombre());
        detalle.put("nuevoTicket", nueva.getNumeroTicket());

        String pacienteLegible = Formato.nombrePaciente(paciente.getUsuario());
        notificacionService.crear(anterior.getDoctor().getUsuario(), Notificacion.SOLICITUD,
                "Tu paciente eligió otra cita",
                pacienteLegible + " canceló su cita del " + Formato.diaHora(anterior.getFecha(), anterior.getHoraInicio())
                        + " y reservó una cita con " + doctor + ".");
        notificacionService.crear(nuevo.getDoctor().getUsuario(), Notificacion.CITA,
                "Nueva cita asignada",
                pacienteLegible + " reservó una cita para el " + cuando + ".");
        return new ResolucionResponse(nueva.getIdCita(), Solicitud.RESUELTA, Solicitud.OTRO_DOCTOR,
                "Reservamos tu cita con " + doctor + " (" + nuevo.getDoctor().getEspecialidad().getNombre()
                        + ") para el " + cuando + ". Tu nuevo ticket es " + nueva.getNumeroTicket()
                        + ". La cita anterior quedó cancelada.",
                nueva.getIdCita(), nueva.getNumeroTicket());
    }

    private ResolucionResponse cancelarCita(Cita cita, Horario anterior, Map<String, Object> detalle,
                                            LocalDateTime ahora) {
        String cuando = Formato.diaHora(anterior.getFecha(), anterior.getHoraInicio());
        citaService.cancelarCita(cita);
        detalle.put("canceladaEn", Formato.fechaHora(ahora));
        notificacionService.crear(anterior.getDoctor().getUsuario(), Notificacion.SOLICITUD,
                "Cita cancelada por el paciente",
                Formato.nombrePaciente(cita.getPaciente().getUsuario()) + " canceló su cita del " + cuando
                        + ". El turno quedó libre.");
        return new ResolucionResponse(cita.getIdCita(), Solicitud.RESUELTA, Solicitud.CANCELADA,
                "Cancelamos tu cita del " + cuando + ". El turno quedó libre.", null, null);
    }

    // ----- Apoyo -----

    /** Bloquea el turno anterior y el elegido (en orden de id) y devuelve el elegido. */
    private Horario turnoElegido(Horario anterior, Integer idHorarioNuevo) {
        if (idHorarioNuevo == null) {
            throw ApiException.datosInvalidos("Elige el nuevo horario de tu cita.");
        }
        List<Horario> bloqueados = horarioRepository.bloquearVarios(
                List.of(anterior.getIdHorario(), idHorarioNuevo));
        return bloqueados.stream()
                .filter(h -> h.getIdHorario().equals(idHorarioNuevo))
                .findFirst()
                .orElseThrow(() -> ApiException.conflicto("Ese horario ya no está disponible. Elige otro."));
    }

    private CambioPendienteResponse pendiente(Solicitud s) {
        Map<String, Object> datos = datosDe(s);
        Horario h = s.getCita().getHorario();
        Doctor doctor = s.getDoctor();
        return new CambioPendienteResponse(
                s.getIdSolicitud(),
                s.getCita().getIdCita(),
                doctor.getIdDoctor(),
                doctor.getEspecialidad().getIdEspecialidad(),
                h.getConsultorio().getIdConsultorio(),
                doctor.getUsuario().getNombreCorto(),
                doctor.getEspecialidad().getNombre(),
                texto(datos.get("fechaOriginal"), Formato.fecha(h.getFecha())),
                texto(datos.get("horaOriginal"), Formato.hora(h.getHoraInicio())),
                texto(datos.get("consultorioOriginal"), h.getConsultorio().getUbicacion()),
                s.getMotivo(),
                Formato.fechaHora(s.getFechaSolicitud()),
                s.getRespuestaAdmin());
    }

    private CambioPacienteResponse fila(Solicitud s) {
        Map<String, Object> datos = datosDe(s);
        Horario h = s.getCita().getHorario();
        return new CambioPacienteResponse(
                s.getIdSolicitud(),
                s.getEstado(),
                Formato.fechaHora(s.getFechaSolicitud()),
                texto(datos.get("fechaOriginal"), Formato.fecha(h.getFecha())),
                texto(datos.get("horaOriginal"), Formato.hora(h.getHoraInicio())),
                DoctorService.detalleLegible(s, objectMapper),
                s.getMotivo(),
                s.getRespuestaAdmin(),
                Solicitud.APROBADA.equals(s.getEstado()) || Solicitud.RESUELTA.equals(s.getEstado()));
    }

    private Map<String, Object> datosDe(Solicitud s) {
        String json = s.getDatosAdicionales();
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            Map<String, Object> datos = objectMapper.readValue(json, Map.class);
            return datos == null ? new LinkedHashMap<>() : datos;
        } catch (RuntimeException e) {
            // Una fila antigua con JSON inválido no debe romper el portal: se muestra sin esos datos.
            return new LinkedHashMap<>();
        }
    }

    private static String texto(Object valor, String porDefecto) {
        if (valor == null) return porDefecto;
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? porDefecto : texto;
    }

    private static boolean puedeVer(UsuarioSesion sesion, Solicitud s) {
        Integer id = sesion.idUsuario();
        return switch (sesion.rol()) {
            case UsuarioSesion.DOCTOR -> s.getDoctor().getUsuario().getIdUsuario().equals(id);
            case UsuarioSesion.PACIENTE -> s.getCita().getPaciente().getUsuario().getIdUsuario().equals(id);
            case UsuarioSesion.ADMINISTRADOR -> true;
            default -> false;
        };
    }

    private Paciente paciente(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);
        return pacienteRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de paciente."));
    }
}
