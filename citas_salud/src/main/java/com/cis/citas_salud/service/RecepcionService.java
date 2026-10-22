package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.ResultadoTicket;
import com.cis.citas_salud.api.dto.AdminDto.TicketResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Auditoria;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.repository.AuditoriaRepository;
import com.cis.citas_salud.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Módulo de recepción (RF-12): el personal busca el ticket que presenta el paciente y registra su llegada.
 * La validación queda en la tabla auditoria, así no cambia el estado de la cita que usa el doctor.
 */
@Service
@Transactional
public class RecepcionService {

    // "A-0154", "a-154", "A0154" o solo "154".
    private static final Pattern TICKET = Pattern.compile("^(?:A-?)?(\\d{1,9})$");

    private final AdminService adminService;
    private final CitaRepository citaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final NotificacionService notificacionService;

    public RecepcionService(AdminService adminService, CitaRepository citaRepository,
                            AuditoriaRepository auditoriaRepository, NotificacionService notificacionService) {
        this.adminService = adminService;
        this.citaRepository = citaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public TicketResponse consultar(UsuarioSesion sesion, String numeroTicket) {
        adminService.personal(sesion);
        Cita cita = buscar(numeroTicket);
        return respuesta(cita, validacion(cita));
    }

    public TicketResponse validar(UsuarioSesion sesion, String numeroTicket) {
        PersonalAdministrativo admin = adminService.personal(sesion);
        Cita cita = buscar(numeroTicket);
        Optional<Auditoria> previa = validacion(cita);
        ResultadoTicket resultado = resultado(cita, previa);
        if (resultado != ResultadoTicket.VALIDO) {
            throw ApiException.conflicto(mensaje(cita, resultado, previa));
        }

        Horario h = cita.getHorario();
        String paciente = Formato.nombrePaciente(cita.getPaciente().getUsuario());
        Auditoria registro = auditoriaRepository.save(new Auditoria(admin.getUsuario(), Auditoria.VALIDAR_TICKET,
                Auditoria.ENTIDAD_CITA, cita.getIdCita(),
                "Ticket " + cita.getNumeroTicket() + " validado en recepción para " + paciente + "."));
        notificacionService.crear(h.getDoctor().getUsuario(), Notificacion.CITA,
                "Paciente en recepción · " + cita.getNumeroTicket(),
                paciente + " llegó para su cita de las " + Formato.hora(h.getHoraInicio()) + ". Recepción lo envió a "
                        + h.getConsultorio().getUbicacion() + ".");
        return respuesta(cita, Optional.of(registro));
    }

    private Cita buscar(String numeroTicket) {
        String ticket = normalizar(numeroTicket);
        return citaRepository.buscarPorTicket(ticket)
                .orElseThrow(() -> ApiException.noEncontrado("No existe ninguna cita con el ticket " + ticket + "."));
    }

    static String normalizar(String numeroTicket) {
        String limpio = numeroTicket == null ? "" : numeroTicket.trim().toUpperCase();
        Matcher m = TICKET.matcher(limpio);
        return m.matches() ? Formato.ticket(Long.parseLong(m.group(1))) : limpio;
    }

    private Optional<Auditoria> validacion(Cita cita) {
        return auditoriaRepository.findFirstByAccionAndEntidadAndIdRegistroAndResultadoOrderByFechaDesc(
                Auditoria.VALIDAR_TICKET, Auditoria.ENTIDAD_CITA, cita.getIdCita(), Auditoria.EXITOSO);
    }

    private static ResultadoTicket resultado(Cita cita, Optional<Auditoria> previa) {
        if (!cita.estaActiva()) return ResultadoTicket.NO_ACTIVA;
        if (!cita.getHorario().getFecha().equals(LocalDate.now())) return ResultadoTicket.OTRO_DIA;
        return previa.isPresent() ? ResultadoTicket.YA_VALIDADO : ResultadoTicket.VALIDO;
    }

    private static String mensaje(Cita cita, ResultadoTicket resultado, Optional<Auditoria> previa) {
        Horario h = cita.getHorario();
        return switch (resultado) {
            case VALIDO -> "Ticket válido. Indica al paciente que se acerque a " + h.getConsultorio().getUbicacion() + ".";
            case YA_VALIDADO -> "Este ticket ya fue validado a las "
                    + previa.map(a -> Formato.hora(a.getFecha().toLocalTime())).orElse("-") + ".";
            case OTRO_DIA -> "No le corresponde el turno: la cita es para el "
                    + Formato.diaHora(h.getFecha(), h.getHoraInicio()) + ".";
            case NO_ACTIVA -> switch (cita.getEstado()) {
                case Cita.CANCELADA -> "La cita fue cancelada. El paciente debe reservar una nueva.";
                case Cita.ATENDIDA -> "Esta cita ya fue atendida.";
                default -> "El paciente no asistió a esta cita.";
            };
        };
    }

    private TicketResponse respuesta(Cita cita, Optional<Auditoria> validacion) {
        Horario h = cita.getHorario();
        ResultadoTicket resultado = resultado(cita, validacion);
        LocalDateTime validadoEn = validacion.map(Auditoria::getFecha).orElse(null);
        return new TicketResponse(
                cita.getIdCita(),
                cita.getNumeroTicket(),
                cita.getEstado(),
                Formato.nombrePaciente(cita.getPaciente().getUsuario()),
                cita.getPaciente().getUsuario().getDni(),
                h.getDoctor().getEspecialidad().getNombre(),
                h.getDoctor().getUsuario().getNombreCorto(),
                Formato.fecha(h.getFecha()),
                Formato.hora(h.getHoraInicio()),
                h.getConsultorio().getUbicacion(),
                resultado,
                mensaje(cita, resultado, validacion),
                validadoEn == null ? null : Formato.fechaHora(validadoEn));
    }
}
