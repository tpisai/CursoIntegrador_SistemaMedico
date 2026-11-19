package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Atencion;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Solicitud;
import com.cis.citas_salud.repository.AtencionRepository;
import com.cis.citas_salud.repository.CitaRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.PersonalAdministrativoRepository;
import com.cis.citas_salud.repository.ReporteClinicoRepository;
import com.cis.citas_salud.repository.SolicitudRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Documentos clínicos en PDF que el doctor descarga desde la página de consulta: ficha de consulta, informe
 * médico, referencia a otro establecimiento y fichas de consulta del paciente.
 */
@Service
public class ReporteClinicoService {

    /** Documentos de una atención. La plantilla está en {@code src/main/resources/reports}. */
    public enum Documento {
        FICHA_CONSULTA("FichaConsultaMedica", "ficha-consulta"),
        INFORME_MEDICO("InformeMedico", "informe-medico"),
        REFERENCIA("InformeMedicoDerivacion", "referencia");

        private final String plantilla;
        private final String archivo;

        Documento(String plantilla, String archivo) {
            this.plantilla = plantilla;
            this.archivo = archivo;
        }
    }

    private static final String FICHAS_DEL_PACIENTE = "FichaConsultaPaciente";

    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final ReporteClinicoRepository reporteRepository;
    private final PacienteRepository pacienteRepository;
    private final AtencionRepository atencionRepository;
    private final SolicitudRepository solicitudRepository;
    private final PersonalAdministrativoRepository personalRepository;
    private final GeneradorReportesSql generador;

    public ReporteClinicoService(DoctorRepository doctorRepository, CitaRepository citaRepository,
                                 ReporteClinicoRepository reporteRepository, PacienteRepository pacienteRepository,
                                 AtencionRepository atencionRepository, SolicitudRepository solicitudRepository,
                                 PersonalAdministrativoRepository personalRepository, GeneradorReportesSql generador) {
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.reporteRepository = reporteRepository;
        this.pacienteRepository = pacienteRepository;
        this.atencionRepository = atencionRepository;
        this.solicitudRepository = solicitudRepository;
        this.personalRepository = personalRepository;
        this.generador = generador;
    }

    /** Documento de una atención que registró el propio doctor. */
    public Archivo deAtencion(UsuarioSesion sesion, Integer idAtencion, Documento documento) {
        Doctor doctor = doctor(sesion);
        if (!reporteRepository.esDelDoctor(idAtencion, doctor.getIdDoctor())) {
            throw ApiException.noEncontrado("No encontramos esa atención.");
        }
        if (documento == Documento.REFERENCIA && !reporteRepository.tieneDerivacion(idAtencion)) {
            throw ApiException.conflicto("Esta atención no tiene una solicitud de derivación.");
        }
        byte[] pdf = generador.pdf(documento.plantilla, Map.of("ID_ATENCION", idAtencion));
        return new Archivo(documento.archivo + "-" + idAtencion + ".pdf", "application/pdf", pdf);
    }

    /** Referencia descargable desde el historial, limitada al paciente propietario de la atención. */
    @Transactional(readOnly = true)
    public Archivo deReferenciaPaciente(UsuarioSesion sesion, Integer idAtencion) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);
        Integer idPaciente = pacienteRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .map(p -> p.getIdPaciente())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de paciente."));
        if (!reporteRepository.esDelPaciente(idAtencion, idPaciente)) {
            throw ApiException.noEncontrado("No encontramos esa atención.");
        }
        if (!reporteRepository.tieneDerivacion(idAtencion)) {
            throw ApiException.noEncontrado("Esta atención no tiene una referencia disponible.");
        }
        byte[] pdf = generador.pdf(Documento.REFERENCIA.plantilla, Map.of("ID_ATENCION", idAtencion));
        return new Archivo(Documento.REFERENCIA.archivo + "-" + idAtencion + ".pdf", "application/pdf", pdf);
    }

    /** Referencia desde una solicitud del panel: solo personal administrativo activo y solicitudes DERIVACION. */
    @Transactional(readOnly = true)
    public Archivo deReferenciaAdministracion(UsuarioSesion sesion, Integer idSolicitud) {
        sesion.exigirRol(UsuarioSesion.ADMINISTRADOR);
        PersonalAdministrativo personal = personalRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de personal administrativo activo."));
        Solicitud solicitud = solicitudRepository.buscarConDetalle(idSolicitud)
                .filter(s -> Solicitud.DERIVACION.equals(s.getTipo()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa derivación."));
        Atencion atencion = atencionRepository.findByCita_IdCita(solicitud.getCita().getIdCita())
                .orElseThrow(() -> ApiException.noEncontrado("La referencia estará disponible cuando se registre la atención."));
        Integer idAtencion = atencion.getIdAtencion();
        byte[] pdf = generador.pdf(Documento.REFERENCIA.plantilla, Map.of("ID_ATENCION", idAtencion));
        return new Archivo(Documento.REFERENCIA.archivo + "-" + idAtencion + ".pdf", "application/pdf", pdf);
    }

    /** Todas las fichas de consulta del paciente de una cita del doctor. */
    public Archivo fichasDelPaciente(UsuarioSesion sesion, Integer idCita) {
        Doctor doctor = doctor(sesion);
        Cita cita = citaRepository.buscarConDetalle(idCita)
                .filter(c -> c.getHorario().getDoctor().getIdDoctor().equals(doctor.getIdDoctor()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa cita en tu agenda."));
        Integer idPaciente = cita.getPaciente().getIdPaciente();
        byte[] pdf = generador.pdf(FICHAS_DEL_PACIENTE, Map.of("ID_PACIENTE", idPaciente));
        return new Archivo("fichas-consulta-" + cita.getPaciente().getUsuario().getDni() + ".pdf", "application/pdf", pdf);
    }

    private Doctor doctor(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.DOCTOR);
        return doctorRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de doctor."));
    }
}
