package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.api.dto.CitaDto.ConsultorioResponse;
import com.cis.citas_salud.api.dto.CitaDto.DisponibilidadResponse;
import com.cis.citas_salud.api.dto.CitaDto.DoctorResponse;
import com.cis.citas_salud.api.dto.CitaDto.EspecialidadResponse;
import com.cis.citas_salud.api.dto.CitaDto.HorarioResponse;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Datos para buscar disponibilidad (RF-05): especialidades, doctores, consultorios y horarios. */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final EspecialidadRepository especialidadRepository;
    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;

    public CatalogoService(EspecialidadRepository especialidadRepository, DoctorRepository doctorRepository,
                           HorarioRepository horarioRepository) {
        this.especialidadRepository = especialidadRepository;
        this.doctorRepository = doctorRepository;
        this.horarioRepository = horarioRepository;
    }

    public List<EspecialidadResponse> especialidades() {
        return especialidadRepository.findByEstadoOrderByIdEspecialidad("ACTIVO").stream()
                .map(e -> new EspecialidadResponse(e.getIdEspecialidad(), e.getNombre()))
                .toList();
    }

    public List<DoctorResponse> doctores(Integer idEspecialidad) {
        return doctorRepository.findByEspecialidad_IdEspecialidadAndEstadoOrderByIdDoctor(idEspecialidad, "ACTIVO").stream()
                .map(d -> new DoctorResponse(d.getIdDoctor(), idEspecialidad, d.getUsuario().getNombreCorto()))
                .toList();
    }

    public List<ConsultorioResponse> consultorios(Integer idDoctor) {
        return doctorRepository.findById(idDoctor)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos a ese doctor."))
                .getConsultorios().stream()
                .map(c -> new ConsultorioResponse(c.getIdConsultorio(), c.getNombre(), c.getZona(), c.getPiso(), c.getNumero()))
                .toList();
    }

    /** Días del mes con cupos libres. mes = "YYYY-MM". */
    public List<DisponibilidadResponse> disponibilidad(Integer idDoctor, Integer idConsultorio, String mes) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(mes);
        } catch (RuntimeException e) {
            throw ApiException.datosInvalidos("El mes debe tener el formato AAAA-MM.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Map<LocalDate, Long> cupos = new TreeMap<>();
        horarioRepository.findByDoctor_IdDoctorAndConsultorio_IdConsultorioAndFechaBetweenAndEstado(
                        idDoctor, idConsultorio, yearMonth.atDay(1), yearMonth.atEndOfMonth(), Horario.DISPONIBLE)
                .stream()
                .filter(h -> h.estaLibre(ahora))
                .forEach(h -> cupos.merge(h.getFecha(), 1L, Long::sum));
        return cupos.entrySet().stream()
                .map(e -> new DisponibilidadResponse(Formato.fecha(e.getKey()), e.getValue()))
                .toList();
    }

    public List<HorarioResponse> horarios(Integer idDoctor, Integer idConsultorio, LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now();
        return horarioRepository.findByDoctor_IdDoctorAndConsultorio_IdConsultorioAndFechaAndEstadoNotOrderByHoraInicio(
                        idDoctor, idConsultorio, fecha, Horario.CANCELADO).stream()
                .map(h -> new HorarioResponse(
                        h.getIdHorario(),
                        Formato.fecha(h.getFecha()),
                        Formato.hora(h.getHoraInicio()),
                        Formato.hora(h.getHoraFin()),
                        // Un turno libre que ya empezó se muestra como finalizado.
                        Horario.DISPONIBLE.equals(h.getEstado()) && h.yaPaso(ahora) ? Horario.FINALIZADO : h.getEstado()))
                .toList();
    }
}
