package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.DoctorDto.CambioFechaRequest;
import com.cis.citas_salud.api.dto.DoctorDto.CitaAsignadaResponse;
import com.cis.citas_salud.api.dto.DoctorDto.DerivacionRequest;
import com.cis.citas_salud.api.dto.DoctorDto.HorarioLibreResponse;
import com.cis.citas_salud.api.dto.DoctorDto.SolicitudResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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

    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public DoctorService(DoctorRepository doctorRepository, CitaRepository citaRepository,
                         HorarioRepository horarioRepository, SolicitudRepository solicitudRepository,
                         UsuarioRepository usuarioRepository, NotificacionService notificacionService,
                         ObjectMapper objectMapper) {
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
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
        if (s.getDatosAdicionales() == null) return null;
        Map<?, ?> datos = objectMapper.readValue(s.getDatosAdicionales(), Map.class);
        if (Solicitud.CAMBIO_HORARIO.equals(s.getTipo())) {
            LocalDate fecha = LocalDate.parse(String.valueOf(datos.get("fecha")));
            return "Nueva fecha: " + Formato.diaHora(fecha, LocalTime.parse(String.valueOf(datos.get("horaInicio"))));
        }
        return datos.get("establecimiento") + " · " + datos.get("especialidad");
    }
}
