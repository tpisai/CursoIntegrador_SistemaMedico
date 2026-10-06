package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AdminDto.CampaniaAdminResponse;
import com.cis.citas_salud.api.dto.AdminDto.CrearCampaniaRequest;
import com.cis.citas_salud.api.dto.CampaniaDto.CampaniaResponse;
import com.cis.citas_salud.api.dto.CampaniaDto.InscripcionResponse;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Campania;
import com.cis.citas_salud.entity.InscripcionCampania;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.PersonalAdministrativo;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.CampaniaRepository;
import com.cis.citas_salud.repository.InscripcionCampaniaRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.service.CitaService.Archivo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Campañas médicas: publicación (RF-20), aviso a pacientes (RF-19) e inscripción (RF-21). */
@Service
@Transactional
public class CampaniaService {

    private final CampaniaRepository campaniaRepository;
    private final InscripcionCampaniaRepository inscripcionRepository;
    private final PacienteRepository pacienteRepository;
    private final AdminService adminService;
    private final NotificacionService notificacionService;

    public CampaniaService(CampaniaRepository campaniaRepository, InscripcionCampaniaRepository inscripcionRepository,
                           PacienteRepository pacienteRepository, AdminService adminService,
                           NotificacionService notificacionService) {
        this.campaniaRepository = campaniaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pacienteRepository = pacienteRepository;
        this.adminService = adminService;
        this.notificacionService = notificacionService;
    }

    // ----- Administración -----

    @Transactional(readOnly = true)
    public List<CampaniaAdminResponse> listarParaAdmin(UsuarioSesion sesion) {
        adminService.personal(sesion);
        Map<Integer, Long> inscritos = new HashMap<>();
        for (Object[] fila : inscripcionRepository.contarInscritos()) {
            inscritos.put((Integer) fila[0], (Long) fila[1]);
        }
        return campaniaRepository.findAllByOrderByFechaInicioDescIdCampaniaDesc().stream()
                .map(c -> respuestaAdmin(c, inscritos.getOrDefault(c.getIdCampania(), 0L)))
                .toList();
    }

    public CampaniaAdminResponse crear(UsuarioSesion sesion, CrearCampaniaRequest datos) {
        PersonalAdministrativo admin = adminService.personal(sesion);
        LocalDate hoy = LocalDate.now();
        if (datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw ApiException.datosInvalidos("La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (datos.fechaFin().isBefore(hoy)) throw ApiException.datosInvalidos("La campaña no puede terminar en el pasado.");

        Campania campania = new Campania();
        campania.setPersonal(admin);
        campania.setTitulo(datos.titulo().trim());
        campania.setDescripcion(textoOpcional(datos.descripcion()));
        campania.setFechaInicio(datos.fechaInicio());
        campania.setFechaFin(datos.fechaFin());
        campania.setHora(datos.hora());
        campania.setLugar(textoOpcional(datos.lugar()));
        campania.setEstado(datos.fechaInicio().isAfter(hoy) ? Campania.PLANIFICADA : Campania.ACTIVA);
        campania.setCupos(datos.cupos());
        campania.setCuposDisponibles(datos.cupos());
        campaniaRepository.save(campania);

        String mensaje = cuando(campania) + (campania.getLugar() != null ? " · " + campania.getLugar() : "")
                + ". Inscríbete desde Comunicación; hay " + campania.getCupos() + " cupos.";
        for (Usuario paciente : pacienteRepository.usuariosActivos()) {
            notificacionService.crear(paciente, Notificacion.CAMPANIA, "Nueva campaña: " + campania.getTitulo(), mensaje);
        }
        return respuestaAdmin(campania, 0);
    }

    // ----- Paciente -----

    @Transactional(readOnly = true)
    public List<CampaniaResponse> vigentes(UsuarioSesion sesion) {
        Paciente paciente = paciente(sesion);
        Map<Integer, InscripcionCampania> inscripciones = new HashMap<>();
        for (InscripcionCampania i : inscripcionRepository.delPaciente(paciente.getIdPaciente())) {
            if (InscripcionCampania.INSCRITO.equals(i.getEstado())) {
                inscripciones.put(i.getCampania().getIdCampania(), i);
            }
        }
        return campaniaRepository.vigentes(Campania.VIGENTES, LocalDate.now()).stream()
                .map(c -> {
                    InscripcionCampania i = inscripciones.get(c.getIdCampania());
                    return new CampaniaResponse(
                            c.getIdCampania(),
                            c.getTitulo(),
                            c.getDescripcion(),
                            Formato.fecha(c.getFechaInicio()),
                            Formato.fecha(c.getFechaFin()),
                            c.getHora() != null ? Formato.hora(c.getHora()) : null,
                            c.getLugar(),
                            c.getEstado(),
                            c.getCupos(),
                            c.getCuposDisponibles(),
                            i != null ? i.getIdInscripcion() : null,
                            i != null ? i.getCodigo() : null);
                })
                .toList();
    }

    public InscripcionResponse inscribir(UsuarioSesion sesion, Integer idCampania) {
        Paciente paciente = paciente(sesion);
        Campania campania = campaniaRepository.findById(idCampania)
                .filter(c -> Campania.VIGENTES.contains(c.getEstado()) && !c.getFechaFin().isBefore(LocalDate.now()))
                .orElseThrow(() -> ApiException.noEncontrado("Esa campaña ya no está disponible."));
        if (inscripcionRepository.existsByCampania_IdCampaniaAndPaciente_IdPaciente(idCampania, paciente.getIdPaciente())) {
            throw ApiException.conflicto("Ya estás inscrito en esta campaña.");
        }
        if (campaniaRepository.tomarCupo(idCampania) == 0) {
            throw ApiException.conflicto("Ya no quedan cupos en esta campaña.");
        }

        InscripcionCampania inscripcion = new InscripcionCampania();
        inscripcion.setCampania(campania);
        inscripcion.setPaciente(paciente);
        try {
            inscripcionRepository.saveAndFlush(inscripcion);
        } catch (DataIntegrityViolationException e) {
            // Dos clics a la vez: uq_campania_paciente impide la segunda; el cupo descontado se revierte.
            throw ApiException.conflicto("Ya estás inscrito en esta campaña.");
        }

        notificacionService.crear(paciente.getUsuario(), Notificacion.CAMPANIA,
                "Inscripción confirmada · " + inscripcion.getCodigo(),
                "Te inscribiste en " + campania.getTitulo() + " (" + cuando(campania) + "). Presenta tu comprobante y tu DNI.");
        return new InscripcionResponse(inscripcion.getIdInscripcion(), inscripcion.getCodigo(), idCampania,
                campania.getTitulo(), Formato.fechaHora(inscripcion.getFechaInscripcion()));
    }

    @Transactional(readOnly = true)
    public Archivo comprobante(UsuarioSesion sesion, Integer idInscripcion) {
        Paciente paciente = paciente(sesion);
        InscripcionCampania i = inscripcionRepository.buscarConDetalle(idInscripcion)
                .filter(x -> x.getPaciente().getIdPaciente().equals(paciente.getIdPaciente()))
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa inscripción."));
        Campania c = i.getCampania();
        Usuario u = paciente.getUsuario();
        byte[] pdf = PdfSimple.crear("Comprobante de inscripción N.° " + i.getCodigo(), List.of(
                "Centro de Salud Miguel Grau · Chaclacayo",
                "",
                "Campaña: " + c.getTitulo(),
                "Fecha: " + cuando(c),
                "Lugar: " + (c.getLugar() != null ? c.getLugar() : "Centro de Salud Miguel Grau"),
                "",
                "Paciente: " + u.getNombres() + " " + u.getApellidos() + " · DNI " + u.getDni(),
                "Inscrito el: " + Formato.fechaLegible(i.getFechaInscripcion().toLocalDate()),
                "",
                "Presenta este comprobante y tu DNI el día de la campaña."));
        return new Archivo("comprobante-" + i.getCodigo() + ".pdf", "application/pdf", pdf);
    }

    private Paciente paciente(UsuarioSesion sesion) {
        sesion.exigirRol(UsuarioSesion.PACIENTE);
        return pacienteRepository.findByUsuario_IdUsuario(sesion.idUsuario())
                .orElseThrow(() -> ApiException.prohibido("Tu cuenta no tiene un perfil de paciente."));
    }

    /** "12/10/2026 · 09:00" o "12/10/2026 al 16/10/2026". */
    private static String cuando(Campania c) {
        String fechas = c.getFechaInicio().equals(c.getFechaFin())
                ? Formato.fechaLegible(c.getFechaInicio())
                : Formato.fechaLegible(c.getFechaInicio()) + " al " + Formato.fechaLegible(c.getFechaFin());
        return c.getHora() != null ? fechas + " · " + Formato.hora(c.getHora()) : fechas;
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static CampaniaAdminResponse respuestaAdmin(Campania c, long inscritos) {
        return new CampaniaAdminResponse(
                c.getIdCampania(),
                c.getTitulo(),
                c.getDescripcion(),
                Formato.fecha(c.getFechaInicio()),
                Formato.fecha(c.getFechaFin()),
                c.getHora() != null ? Formato.hora(c.getHora()) : null,
                c.getLugar(),
                c.getEstado(),
                c.getCupos(),
                c.getCuposDisponibles(),
                inscritos);
    }
}
