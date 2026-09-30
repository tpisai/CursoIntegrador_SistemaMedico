package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.AuthDto.LoginRequest;
import com.cis.citas_salud.api.dto.AuthDto.RecuperarPinRequest;
import com.cis.citas_salud.api.dto.AuthDto.RegistroRequest;
import com.cis.citas_salud.api.dto.AuthDto.SesionResponse;
import com.cis.citas_salud.api.dto.AuthDto.TipoCuenta;
import com.cis.citas_salud.api.dto.AuthDto.UsuarioResponse;
import com.cis.citas_salud.api.seguridad.TokenService;
import com.cis.citas_salud.api.seguridad.UsuarioSesion;
import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.HistoriaClinica;
import com.cis.citas_salud.entity.Notificacion;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Usuario;
import com.cis.citas_salud.repository.ConsultorioRepository;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.HistoriaClinicaRepository;
import com.cis.citas_salud.repository.PacienteRepository;
import com.cis.citas_salud.repository.RolRepository;
import com.cis.citas_salud.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_INTENTOS = 3;
    private static final Duration BLOQUEO = Duration.ofMinutes(5);
    private static final int MAX_DOCTORES_POR_CONSULTORIO = 2;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultorioRepository consultorioRepository;
    private final HorarioGeneradorService horarioGenerador;
    private final NotificacionService notificacionService;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder pinEncoder;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       PacienteRepository pacienteRepository, HistoriaClinicaRepository historiaClinicaRepository,
                       DoctorRepository doctorRepository, EspecialidadRepository especialidadRepository,
                       ConsultorioRepository consultorioRepository, HorarioGeneradorService horarioGenerador,
                       NotificacionService notificacionService, TokenService tokenService,
                       BCryptPasswordEncoder pinEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.pacienteRepository = pacienteRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.consultorioRepository = consultorioRepository;
        this.horarioGenerador = horarioGenerador;
        this.notificacionService = notificacionService;
        this.tokenService = tokenService;
        this.pinEncoder = pinEncoder;
    }

    // noRollbackFor: los intentos fallidos y el bloqueo deben guardarse aunque se responda con error.
    @Transactional(noRollbackFor = ApiException.class)
    public SesionResponse login(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByDni(datos.dni())
                .orElseThrow(() -> ApiException.noAutorizado("DNI o PIN incorrectos."));

        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw ApiException.prohibido("Tu cuenta está desactivada. Acércate a admisión del centro de salud.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            long minutos = Math.max(1, Duration.between(ahora, usuario.getBloqueadoHasta()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED,
                    "Tu cuenta está bloqueada temporalmente. Inténtalo de nuevo en " + minutos + " min o recupera tu PIN.");
        }

        if (usuario.getPinHash() == null || !pinEncoder.matches(datos.pin(), usuario.getPinHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            if (intentos >= MAX_INTENTOS) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(ahora.plus(BLOQUEO));
                throw new ApiException(HttpStatus.LOCKED, "Bloqueamos tu cuenta por " + BLOQUEO.toMinutes()
                        + " minutos tras " + MAX_INTENTOS + " intentos fallidos. Puedes recuperar tu PIN por correo.");
            }
            usuario.setIntentosFallidos(intentos);
            int restantes = MAX_INTENTOS - intentos;
            throw ApiException.noAutorizado("DNI o PIN incorrectos. Te "
                    + (restantes == 1 ? "queda 1 intento." : "quedan " + restantes + " intentos."));
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        return sesionPara(usuario);
    }

    public SesionResponse registrar(RegistroRequest datos) {
        if (usuarioRepository.existsByDni(datos.dni())) {
            throw ApiException.conflicto("Ya existe una cuenta registrada con este DNI.");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(datos.correo().trim())) {
            throw ApiException.conflicto("Este correo ya está asociado a otra cuenta.");
        }

        Usuario usuario = new Usuario();
        usuario.setRol(rolRepository.findByNombre(datos.tipoCuenta().name())
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + datos.tipoCuenta() + " (sql/03_datos_iniciales.sql)")));
        usuario.setDni(datos.dni());
        usuario.setNombres(datos.nombres().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setCorreo(datos.correo().trim().toLowerCase());
        usuario.setPinHash(pinEncoder.encode(datos.pin()));
        usuario.setTerminosAceptados(true);

        if (datos.tipoCuenta() == TipoCuenta.DOCTOR) {
            registrarDoctor(usuarioRepository.save(usuario), datos);
        } else {
            registrarPaciente(usuarioRepository.save(usuario));
        }

        notificacionService.crear(usuario, Notificacion.SISTEMA, "Te damos la bienvenida a SaludGrau",
                datos.tipoCuenta() == TipoCuenta.DOCTOR
                        ? "Tu cuenta de doctor está lista. En tu panel verás las citas que te asignen."
                        : "Desde aquí puedes reservar tus citas, ver tu ticket de atención y descargar tus documentos médicos.");
        return sesionPara(usuario);
    }

    private void registrarPaciente(Usuario usuario) {
        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        pacienteRepository.save(paciente);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setNumeroHistoria("HC-" + String.format("%06d", paciente.getIdPaciente()));
        historiaClinicaRepository.save(historia);
    }

    private void registrarDoctor(Usuario usuario, RegistroRequest datos) {
        if (datos.idEspecialidad() == null) throw ApiException.datosInvalidos("Elige tu especialidad.");
        if (datos.cmp() == null || datos.cmp().isBlank()) throw ApiException.datosInvalidos("Ingresa tu número de CMP.");
        if (doctorRepository.existsByCmp(datos.cmp())) {
            throw ApiException.conflicto("Ya existe un doctor registrado con ese CMP.");
        }
        Especialidad especialidad = especialidadRepository.findById(datos.idEspecialidad())
                .orElseThrow(() -> ApiException.datosInvalidos("La especialidad elegida no existe."));

        Doctor doctor = new Doctor();
        doctor.setUsuario(usuario);
        doctor.setEspecialidad(especialidad);
        doctor.setCmp(datos.cmp());
        consultorioConEspacio().ifPresent(doctor.getConsultorios()::add);
        doctorRepository.save(doctor);
        horarioGenerador.generarPara(doctor);
    }

    /** Consultorio activo con menos doctores y que no llegue al máximo de 2. */
    private java.util.Optional<Consultorio> consultorioConEspacio() {
        Map<Integer, Integer> ocupacion = new HashMap<>();
        doctorRepository.findByEstado("ACTIVO").forEach(d ->
                d.getConsultorios().forEach(c -> ocupacion.merge(c.getIdConsultorio(), 1, Integer::sum)));
        return consultorioRepository.findAll().stream()
                .filter(c -> "ACTIVO".equals(c.getEstado()))
                .filter(c -> ocupacion.getOrDefault(c.getIdConsultorio(), 0) < MAX_DOCTORES_POR_CONSULTORIO)
                .min(Comparator.comparing((Consultorio c) -> ocupacion.getOrDefault(c.getIdConsultorio(), 0))
                        .thenComparing(Consultorio::getIdConsultorio));
    }

    public void recuperarPin(RecuperarPinRequest datos) {
        // Se responde igual exista o no el correo, para no revelar qué correos están registrados.
        usuarioRepository.findByCorreoIgnoreCase(datos.correo().trim()).ifPresent(usuario ->
                log.info("Solicitud de recuperación de PIN para el usuario {} (el envío de correo aún no está configurado)",
                        usuario.getIdUsuario()));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(UsuarioSesion sesion) {
        return usuarioRepository.findById(sesion.idUsuario())
                .map(this::respuesta)
                .orElseThrow(() -> ApiException.noAutorizado("Tu sesión expiró. Vuelve a iniciar sesión."));
    }

    private SesionResponse sesionPara(Usuario usuario) {
        return new SesionResponse(tokenService.emitir(usuario.getIdUsuario(), usuario.getRol().getNombre()), respuesta(usuario));
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        String especialidad = UsuarioSesion.DOCTOR.equals(usuario.getRol().getNombre())
                ? doctorRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                        .map(d -> d.getEspecialidad().getNombre()).orElse(null)
                : null;
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getDni(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getCorreo(), usuario.getRol().getNombre(), especialidad);
    }
}
