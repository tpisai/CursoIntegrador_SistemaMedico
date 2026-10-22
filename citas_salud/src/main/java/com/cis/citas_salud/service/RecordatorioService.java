package com.cis.citas_salud.service;

import com.cis.citas_salud.entity.Campania;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.InscripcionCampania;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.InscripcionCampaniaRepository;
import com.cis.citas_salud.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Recordatorios automáticos (RF-22, EDT 4.12): avisa a cada paciente de sus citas y campañas del día
 * siguiente, en la campana del sistema y por correo. Corre al iniciar y luego según
 * saludgrau.recordatorios.cron; el título de cada aviso evita repetirlo en las siguientes pasadas.
 */
@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);
    private static final int MAX_TITULO = 150; // notificacion.titulo VARCHAR(150)

    private final CitaRepository citaRepository;
    private final InscripcionCampaniaRepository inscripcionRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;
    private final CorreoService correoService;

    public RecordatorioService(CitaRepository citaRepository, InscripcionCampaniaRepository inscripcionRepository,
                               NotificacionRepository notificacionRepository, NotificacionService notificacionService,
                               CorreoService correoService) {
        this.citaRepository = citaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.notificacionRepository = notificacionRepository;
        this.notificacionService = notificacionService;
        this.correoService = correoService;
    }

    // void: un @EventListener que devuelve un valor lo publica como un evento nuevo.
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${saludgrau.recordatorios.cron:0 0 * * * *}")
    @Transactional
    public void enviarPendientes() {
        LocalDate manana = LocalDate.now().plusDays(1);
        int citas = 0;
        for (Cita cita : citaRepository.delDia(Cita.ACTIVAS, manana)) {
            if (recordarCita(cita)) citas++;
        }
        int campanias = 0;
        for (InscripcionCampania inscripcion : inscripcionRepository.deCampaniasQueEmpiezan(Campania.VIGENTES, manana)) {
            if (recordarCampania(inscripcion)) campanias++;
        }
        if (citas + campanias > 0) log.info("Recordatorios enviados: {} de citas y {} de campañas", citas, campanias);
    }

    private boolean recordarCita(Cita cita) {
        Horario h = cita.getHorario();
        String mensaje = "Mañana " + Formato.diaHora(h.getFecha(), h.getHoraInicio()) + " tienes cita de "
                + h.getDoctor().getEspecialidad().getNombre() + " con " + h.getDoctor().getUsuario().getNombreCorto()
                + " en " + h.getConsultorio().getUbicacion() + ". Presenta tu ticket " + cita.getNumeroTicket()
                + " en recepción. Si no podrás asistir, cancela la cita para liberar el turno.";
        return recordar(cita.getPaciente().getUsuario(), "Recordatorio de cita · " + cita.getNumeroTicket(), mensaje);
    }

    private boolean recordarCampania(InscripcionCampania inscripcion) {
        Campania c = inscripcion.getCampania();
        String cuando = c.getHora() == null ? "mañana" : "mañana a las " + Formato.hora(c.getHora());
        String donde = c.getLugar() == null || c.getLugar().isBlank() ? "" : " en " + c.getLugar();
        String mensaje = "La campaña \"" + c.getTitulo() + "\" empieza " + cuando + donde
                + ". Lleva tu comprobante " + inscripcion.getCodigo() + " y tu DNI.";
        return recordar(inscripcion.getPaciente().getUsuario(), "Recordatorio de campaña · " + c.getTitulo(), mensaje);
    }

    /** Crea el aviso si aún no existe y envía la copia por correo. */
    private boolean recordar(Usuario usuario, String titulo, String mensaje) {
        String tituloFinal = titulo.length() > MAX_TITULO ? titulo.substring(0, MAX_TITULO - 1) + "…" : titulo;
        if (notificacionRepository.existsByUsuario_IdUsuarioAndTipoAndTitulo(
                usuario.getIdUsuario(), Notificacion.RECORDATORIO, tituloFinal)) {
            return false;
        }
        notificacionService.crear(usuario, Notificacion.RECORDATORIO, tituloFinal, mensaje);
        correoService.enviar(usuario, tituloFinal, mensaje);
        return true;
    }
}
