package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.AtencionHistorialResponse;
import com.cis.citas_salud.api.dto.CitaDto.CitaResponse;
import com.cis.citas_salud.api.dto.CitaDto.DocumentoResponse;
import com.cis.citas_salud.api.dto.CitaDto.ReservarCitaRequest;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.DocumentoMedico;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DocumentoMedicoRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Citas del paciente: reserva (RF-06), ticket (RF-08), historial (RF-09) y documentos (RF-11). */
@Service
@Transactional
public class CitaService {

    public record Archivo(String nombre, String tipoMime, byte[] contenido) {
    }

    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final PacienteRepository pacienteRepository;
    private final DocumentoMedicoRepository documentoRepository;
    private final NotificacionService notificacionService;
    private final Path carpetaArchivos;

    public CitaService(CitaRepository citaRepository, HorarioRepository horarioRepository,
                       PacienteRepository pacienteRepository, DocumentoMedicoRepository documentoRepository,
                       NotificacionService notificacionService,
                       @Value("${saludgrau.archivos.carpeta:archivos}") String carpetaArchivos) {
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionService = notificacionService;
        this.carpetaArchivos = Path.of(carpetaArchivos).toAbsolutePath().normalize();
    }

    public CitaResponse reservar(UsuarioSesion sesion, ReservarCitaRequest datos) {
        Paciente paciente = paciente(sesion);
        Horario horario = horarioRepository.bloquear(datos.idHorario())
                .orElseThrow(() -> ApiException.noEncontrado("Ese horario no existe."));
        if (!horario.estaLibre(LocalDateTime.now())) {
            throw ApiException.conflicto("Este horario acaba de ser reservado por otro paciente. Elige otro horario.");
        }

        horario.setEstado(Horario.RESERVADO);
        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setHorario(horario);
        cita.setEstado(Cita.CONFIRMADA);
        cita.setMotivo(datos.motivo() == null || datos.motivo().isBlank() ? null : datos.motivo().trim());
        cita.setNumeroTicket(Formato.ticket(citaRepository.siguienteTicket()));
        citaRepository.save(cita);

        Cita detalle = citaRepository.buscarConDetalle(cita.getIdCita()).orElseThrow();
        String cuando = Formato.diaHora(horario.getFecha(), horario.getHoraInicio());
        String especialidad = detalle.getHorario().getDoctor().getEspecialidad().getNombre();
        notificacionService.crear(paciente.getUsuario(), Notificacion.CITA,
                "Cita confirmada · " + cita.getNumeroTicket(),
                especialidad + " con " + detalle.getHorario().getDoctor().getUsuario().getNombreCorto() + " el " + cuando
                        + " en " + horario.getConsultorio().getUbicacion() + ". Presenta tu ticket en recepción.");
        notificacionService.crear(detalle.getHorario().getDoctor().getUsuario(), Notificacion.CITA,
                "Nueva cita asignada",
                Formato.nombrePaciente(paciente.getUsuario()) + " reservó una cita para el " + cuando + ".");
        return respuesta(detalle);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> misCitas(UsuarioSesion sesion) {
        return citaRepository.delPacienteDesde(paciente(sesion).getIdPaciente(), Cita.ACTIVAS, LocalDate.now())
                .stream().map(this::respuesta).toList();
    }

    public CitaResponse cancelar(UsuarioSesion sesion, Integer idCita) {
        Cita cita = citaDelPaciente(sesion, idCita);
        if (!cita.estaActiva()) throw ApiException.conflicto("Esta cita ya no se puede cancelar.");

        cita.setEstado(Cita.CANCELADA);
        Horario horario = cita.getHorario();
        horario.setEstado(Horario.CANCELADO);
        // cita.id_horario es UNIQUE: el horario queda con la cita cancelada y se abre uno nuevo para el mismo turno.
        if (!horario.yaPaso(LocalDateTime.now())) {
            horarioRepository.save(new Horario(horario.getDoctor(), horario.getConsultorio(), horario.getFecha(),
                    horario.getHoraInicio(), horario.getHoraFin()));
        }

        notificacionService.crear(horario.getDoctor().getUsuario(), Notificacion.CITA,
                "Cita cancelada por el paciente",
                Formato.nombrePaciente(cita.getPaciente().getUsuario()) + " canceló su cita del "
                        + Formato.diaHora(horario.getFecha(), horario.getHoraInicio()) + ". El turno quedó libre.");
        return respuesta(cita);
    }

    @Transactional(readOnly = true)
    public Archivo ticket(UsuarioSesion sesion, Integer idCita) {
        Cita cita = citaRepository.buscarConDetalle(idCita)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita."));
        Usuario paciente = cita.getPaciente().getUsuario();
        Usuario doctor = cita.getHorario().getDoctor().getUsuario();
        if (!paciente.getIdUsuario().equals(sesion.idUsuario()) && !doctor.getIdUsuario().equals(sesion.idUsuario())) {
            throw ApiException.noEncontrado("No encontramos esa cita.");
        }
        Horario h = cita.getHorario();
        byte[] pdf = PdfSimple.crear("Ticket de atención N.° " + cita.getNumeroTicket(), List.of(
                "Centro de Salud Miguel Grau · Chaclacayo",
                "",
                "Paciente: " + paciente.getNombres() + " " + paciente.getApellidos() + " · DNI " + paciente.getDni(),
                "Especialidad: " + h.getDoctor().getEspecialidad().getNombre(),
                "Profesional: " + doctor.getNombreCorto(),
                "Fecha y hora: " + Formato.diaHora(h.getFecha(), h.getHoraInicio()),
                "Ubicación: " + h.getConsultorio().getUbicacion(),
                "",
                "Presenta este número en recepción para validar tu cita."));
        return new Archivo("ticket-" + cita.getNumeroTicket() + ".pdf", "application/pdf", pdf);
    }

    @Transactional(readOnly = true)
    public List<AtencionHistorialResponse> historial(UsuarioSesion sesion) {
        return citaRepository.historialDelPaciente(paciente(sesion).getIdPaciente(), Cita.CERRADAS).stream()
                .map(c -> new AtencionHistorialResponse(
                        c.getIdCita(),
                        Formato.fecha(c.getHorario().getFecha()),
                        c.getHorario().getDoctor().getEspecialidad().getNombre(),
                        c.getHorario().getDoctor().getUsuario().getNombreCorto(),
                        c.getEstado()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> documentos(UsuarioSesion sesion) {
        return documentoRepository.delPaciente(paciente(sesion).getIdPaciente()).stream()
                .map(d -> new DocumentoResponse(d.getIdDocumento(), d.getTipoDocumento(), d.getNombreArchivo(),
                        Formato.fecha(d.getFechaCarga().toLocalDate())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Archivo documento(UsuarioSesion sesion, Integer idDocumento) {
        Paciente paciente = paciente(sesion);
        DocumentoMedico doc = documentoRepository.findById(idDocumento)
                .filter(d -> d.getAtencion().getHistoria().getPaciente().getIdPaciente().equals(paciente.getIdPaciente()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese documento."));

        Path ruta = carpetaArchivos.resolve(doc.getRutaArchivo()).normalize();
        String tipoMime = doc.getTipoMime() != null ? doc.getTipoMime() : "application/octet-stream";
        if (ruta.startsWith(carpetaArchivos) && Files.isRegularFile(ruta)) {
            try {
                return new Archivo(doc.getNombreArchivo(), tipoMime, Files.readAllBytes(ruta));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // Los documentos de demostración (sql/03_datos_iniciales.sql) no tienen archivo: se genera un PDF de muestra.
        Usuario u = paciente.getUsuario();
        byte[] pdf = PdfSimple.crear(doc.getTipoDocumento(), List.of(
                "Centro de Salud Miguel Grau · Chaclacayo",
                "",
                "Paciente: " + u.getNombres() + " " + u.getApellidos() + " · DNI " + u.getDni(),
                "Fecha de carga: " + Formato.fechaLegible(doc.getFechaCarga().toLocalDate()),
                "Diagnóstico: " + valorOGuion(doc.getAtencion().getDiagnostico()),
                "Tratamiento: " + valorOGuion(doc.getAtencion().getTratamiento()),
                "",
                "Documento de demostración: el archivo original lo cargará el personal de salud."));
        return new Archivo(doc.getNombreArchivo(), "application/pdf", pdf);
    }

    private static String valorOGuion(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private Paciente paciente(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);
        return pacienteRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de paciente."));
    }

    private Cita citaDelPaciente(UsuarioSesion sesion, Integer idCita) {
        Paciente paciente = paciente(sesion);
        return citaRepository.buscarConDetalle(idCita)
                .filter(c -> c.getPaciente().getIdPaciente().equals(paciente.getIdPaciente()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita."));
    }

    CitaResponse respuesta(Cita cita) {
        Horario h = cita.getHorario();
        return new CitaResponse(
                cita.getIdCita(),
                cita.getNumeroTicket(),
                cita.getEstado(),
                Formato.fecha(h.getFecha()),
                Formato.hora(h.getHoraInicio()),
                Formato.hora(h.getHoraFin()),
                h.getDoctor().getEspecialidad().getNombre(),
                h.getDoctor().getUsuario().getNombreCorto(),
                h.getConsultorio().getUbicacion());
    }
}
