package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosRequest;
import com.cis.citas_salud.api.dto.AdminDto.CrearHorariosResponse;
import com.cis.citas_salud.api.dto.AdminDto.Decision;
import com.cis.citas_salud.api.dto.AdminDto.ResponderSolicitudRequest;
import com.cis.citas_salud.api.dto.AdminDto.ResumenResponse;
import com.cis.citas_salud.api.dto.AdminDto.SolicitudAdminResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Campania;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.CampaniaRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PersonalAdministrativoRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Panel de administración y recepción (pantalla 04): RF-13, RF-17 y RF-18. */
@Service
@Transactional
public class AdminService {

    private static final String APROBADA = "APROBADA";
    private static final String RECHAZADA = "RECHAZADA";
    private static final int MAX_TURNOS = 48;
    private static final int MESES_A_FUTURO = 6;

    private final PersonalAdministrativoRepository personalRepository;
    private final CitaRepository citaRepository;
    private final SolicitudRepository solicitudRepository;
    private final CampaniaRepository campaniaRepository;
    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;
    private final HorarioGeneradorService horarioGenerador;
    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public AdminService(PersonalAdministrativoRepository personalRepository, CitaRepository citaRepository,
                        SolicitudRepository solicitudRepository, CampaniaRepository campaniaRepository,
                        DoctorRepository doctorRepository, HorarioRepository horarioRepository,
                        HorarioGeneradorService horarioGenerador, NotificacionService notificacionService,
                        ObjectMapper objectMapper) {
        this.personalRepository = personalRepository;
        this.citaRepository = citaRepository;
        this.solicitudRepository = solicitudRepository;
        this.campaniaRepository = campaniaRepository;
        this.doctorRepository = doctorRepository;
        this.horarioRepository = horarioRepository;
        this.horarioGenerador = horarioGenerador;
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(UsuarioSesion sesion) {
        personal(sesion);
        LocalDate hoy = LocalDate.now();
        long citasHoy = citaRepository.contarEntre(
                List.of(Cita.RESERVADA, Cita.CONFIRMADA, Cita.ATENDIDA, Cita.NO_ASISTIO), hoy, hoy);
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        long cerradas = citaRepository.contarEntre(Cita.CERRADAS, inicioMes, hoy);
        long noAsistio = citaRepository.contarEntre(List.of(Cita.NO_ASISTIO), inicioMes, hoy);
        return new ResumenResponse(
                citasHoy,
                solicitudRepository.countByEstado(Solicitud.PENDIENTE),
                campaniaRepository.countByEstadoInAndFechaFinGreaterThanEqual(Campania.VIGENTES, hoy),
                cerradas == 0 ? 0 : (int) Math.round(100.0 * noAsistio / cerradas));
    }

    @Transactional(readOnly = true)
    public List<SolicitudAdminResponse> solicitudesPendientes(UsuarioSesion sesion) {
        personal(sesion);
        return solicitudRepository.conEstado(Solicitud.PENDIENTE).stream().map(this::respuesta).toList();
    }

    public SolicitudAdminResponse responder(UsuarioSesion sesion, Integer idSolicitud, ResponderSolicitudRequest datos) {
        PersonalAdministrativo admin = personal(sesion);
        // Primero la solicitud y después los horarios, siempre en ese orden (evita bloqueos cruzados).
        Solicitud solicitud = solicitudRepository.bloquear(idSolicitud)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa solicitud."));
        if (!Solicitud.PENDIENTE.equals(solicitud.getEstado())) {
            throw ApiException.conflicto("Esta solicitud ya fue respondida por otra persona.");
        }
        Cita cita = citaRepository.buscarConDetalle(solicitud.getCita().getIdCita()).orElseThrow();
        Doctor doctor = cita.getHorario().getDoctor();
        String respuesta = datos.respuesta() == null || datos.respuesta().isBlank() ? null : datos.respuesta().trim();
        String paciente = Formato.nombrePaciente(cita.getPaciente().getUsuario());
        boolean esCambio = Solicitud.CAMBIO_HORARIO.equals(solicitud.getTipo());
        String queSePidio = esCambio ? "el cambio de fecha de la cita de " + paciente : "la derivación de " + paciente;

        if (datos.decision() == Decision.RECHAZAR) {
            if (respuesta == null) throw ApiException.datosInvalidos("Escribe el motivo del rechazo para el doctor.");
            solicitud.setEstado(RECHAZADA);
            notificacionService.crear(doctor.getUsuario(), Notificacion.SOLICITUD, "Solicitud rechazada",
                    "Administración rechazó " + queSePidio + ". Motivo: " + respuesta);
        } else {
            if (!cita.estaActiva()) {
                throw ApiException.conflicto("La cita ya no está activa (fue atendida o cancelada). Rechaza la solicitud.");
            }
            Map<?, ?> detalle = objectMapper.readValue(solicitud.getDatosAdicionales(), Map.class);
            if (esCambio) {
                reprogramar(cita, ((Number) detalle.get("idHorarioNuevo")).intValue());
            } else {
                notificacionService.crear(cita.getPaciente().getUsuario(), Notificacion.SOLICITUD,
                        "Fuiste derivado a otro establecimiento",
                        "Tu médico te derivó a " + detalle.get("establecimiento") + " para " + detalle.get("especialidad")
                                + ". Acércate a admisión para recoger tu hoja de referencia.");
            }
            solicitud.setEstado(APROBADA);
            notificacionService.crear(doctor.getUsuario(), Notificacion.SOLICITUD, "Solicitud aprobada",
                    "Administración aprobó " + queSePidio + " y avisó al paciente.");
        }

        solicitud.setRespuestaAdmin(respuesta);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitud.setIdPersonalRevisor(admin.getIdPersonalAdministrativo());
        return respuesta(solicitud);
    }

    /** Mueve la cita al horario propuesto: el anterior queda libre y el nuevo, reservado. */
    private void reprogramar(Cita cita, Integer idHorarioNuevo) {
        Horario anterior = cita.getHorario();
        List<Horario> bloqueados = horarioRepository.bloquearVarios(List.of(anterior.getIdHorario(), idHorarioNuevo));
        Horario nuevo = bloqueados.stream().filter(h -> h.getIdHorario().equals(idHorarioNuevo)).findFirst()
                .orElseThrow(() -> ApiException.conflicto("El horario propuesto ya no existe. Rechaza la solicitud."));
        if (!nuevo.estaLibre(LocalDateTime.now())) {
            throw ApiException.conflicto("El horario propuesto ya fue tomado por otro paciente. Rechaza la solicitud.");
        }
        String antes = Formato.diaHora(anterior.getFecha(), anterior.getHoraInicio());

        anterior.setEstado(anterior.yaPaso(LocalDateTime.now()) ? Horario.FINALIZADO : Horario.DISPONIBLE);
        nuevo.setEstado(Horario.RESERVADO);
        cita.setHorario(nuevo);

        notificacionService.crear(cita.getPaciente().getUsuario(), Notificacion.CITA, "Tu cita fue reprogramada",
                "Tu cita de " + nuevo.getDoctor().getEspecialidad().getNombre() + " pasó del " + antes + " al "
                        + Formato.diaHora(nuevo.getFecha(), nuevo.getHoraInicio()) + " en "
                        + nuevo.getConsultorio().getUbicacion() + ". Tu ticket " + cita.getNumeroTicket() + " sigue siendo válido.");
    }

    public CrearHorariosResponse crearHorarios(UsuarioSesion sesion, CrearHorariosRequest datos) {
        personal(sesion);
        LocalDate hoy = LocalDate.now();
        if (datos.fecha().isBefore(hoy)) throw ApiException.datosInvalidos("No se pueden crear horarios en fechas pasadas.");
        if (datos.fecha().isAfter(hoy.plusMonths(MESES_A_FUTURO))) {
            throw ApiException.datosInvalidos("Solo se pueden crear horarios hasta " + MESES_A_FUTURO + " meses adelante.");
        }
        if (!datos.horaFin().isAfter(datos.horaInicio())) {
            throw ApiException.datosInvalidos("La hora de fin debe ser posterior a la hora de inicio.");
        }
        long turnos = Duration.between(datos.horaInicio(), datos.horaFin()).toMinutes() / datos.duracionMinutos();
        if (turnos == 0) throw ApiException.datosInvalidos("El rango de horas es más corto que un turno.");
        if (turnos > MAX_TURNOS) throw ApiException.datosInvalidos("Puedes crear como máximo " + MAX_TURNOS + " turnos a la vez.");
        if (datos.fecha().equals(hoy) && !datos.horaInicio().isAfter(LocalTime.now())) {
            throw ApiException.datosInvalidos("Para hoy, la hora de inicio debe ser posterior a la hora actual.");
        }

        Doctor doctor = doctorRepository.findById(datos.idDoctor())
                .filter(d -> "ACTIVO".equals(d.getEstado()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos a ese doctor."));
        Consultorio consultorio = doctor.getConsultorios().stream()
                .filter(c -> c.getIdConsultorio().equals(datos.idConsultorio()))
                .findFirst()
                .orElseThrow(() -> ApiException.datosInvalidos("Ese consultorio no está asignado al doctor."));

        HorarioGeneradorService.Resultado resultado = horarioGenerador.crearTurnos(doctor, consultorio, datos.fecha(),
                datos.horaInicio(), datos.horaFin(), datos.duracionMinutos());
        return new CrearHorariosResponse(resultado.creados(), resultado.omitidos(), Formato.fecha(datos.fecha()),
                doctor.getUsuario().getNombreCorto(), consultorio.getUbicacion());
    }

    PersonalAdministrativo personal(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.ADMINISTRADOR);
        return personalRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de personal administrativo activo."));
    }

    private SolicitudAdminResponse respuesta(Solicitud s) {
        Cita cita = s.getCita();
        Horario h = cita.getHorario();
        Doctor d = s.getDoctor();
        return new SolicitudAdminResponse(
                s.getIdSolicitud(),
                s.getTipo(),
                s.getEstado(),
                Formato.fechaHora(s.getFechaSolicitud()),
                d.getUsuario().getNombreCorto(),
                d.getEspecialidad().getNombre(),
                Formato.nombrePaciente(cita.getPaciente().getUsuario()),
                cita.getPaciente().getUsuario().getDni(),
                Formato.fecha(h.getFecha()),
                Formato.hora(h.getHoraInicio()),
                DoctorService.detalleLegible(s, objectMapper),
                s.getMotivo(),
                s.getRespuestaAdmin());
    }
}
