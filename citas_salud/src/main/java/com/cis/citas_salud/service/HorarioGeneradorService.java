package com.cis.citas_salud.service;

import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.repository.DoctorRepository;
import com.cis.citas_salud.repository.HorarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crea los turnos libres de los próximos días para cada doctor activo.
 * Reemplaza temporalmente a "crear horarios" de admisión (RF-13) hasta que exista ese panel.
 * Turnos de 30 min: lunes a viernes 08:00-12:00, sábado 08:00-10:00, domingo sin consulta.
 */
@Service
public class HorarioGeneradorService {

    private static final Logger log = LoggerFactory.getLogger(HorarioGeneradorService.class);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final int MINUTOS_TURNO = 30;
    private static final int TURNOS_SEMANA = 8;
    private static final int TURNOS_SABADO = 4;

    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;
    private final boolean activo;
    private final int diasAFuturo;

    public HorarioGeneradorService(DoctorRepository doctorRepository,
                                   HorarioRepository horarioRepository,
                                   @Value("${saludgrau.horarios.autogenerar:true}") boolean activo,
                                   @Value("${saludgrau.horarios.dias-a-futuro:60}") int diasAFuturo) {
        this.doctorRepository = doctorRepository;
        this.horarioRepository = horarioRepository;
        this.activo = activo;
        this.diasAFuturo = diasAFuturo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generarAlIniciar() {
        if (!activo) return;
        int creados = 0;
        for (Doctor doctor : doctorRepository.findByEstado("ACTIVO")) {
            creados += generarPara(doctor);
        }
        log.info("Horarios generados al iniciar: {}", creados);
    }

    /** Consultorio donde atiende el doctor ese día: rota por día de la semana (igual que 03_datos_iniciales.sql). */
    public static Consultorio consultorioDelDia(Doctor doctor, LocalDate fecha) {
        List<Consultorio> consultorios = doctor.getConsultorios();
        int diaSemana = fecha.getDayOfWeek().getValue() % 7; // domingo = 0, como extract(dow) en PostgreSQL
        return consultorios.get(diaSemana % consultorios.size());
    }

    @Transactional
    public int generarPara(Doctor doctor) {
        if (!activo || doctor.getConsultorios().isEmpty()) return 0;

        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(diasAFuturo);
        Set<String> existentes = new HashSet<>();
        for (Horario h : horarioRepository.findByDoctor_IdDoctorAndFechaBetweenAndEstadoNot(
                doctor.getIdDoctor(), desde, hasta, Horario.CANCELADO)) {
            existentes.add(h.getFecha() + " " + h.getHoraInicio());
        }

        List<Horario> nuevos = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            DayOfWeek dia = fecha.getDayOfWeek();
            if (dia == DayOfWeek.SUNDAY) continue;
            int turnos = dia == DayOfWeek.SATURDAY ? TURNOS_SABADO : TURNOS_SEMANA;
            Consultorio consultorio = consultorioDelDia(doctor, fecha);
            for (int i = 0; i < turnos; i++) {
                LocalTime inicio = INICIO.plusMinutes((long) i * MINUTOS_TURNO);
                if (existentes.contains(fecha + " " + inicio)) continue;
                nuevos.add(new Horario(doctor, consultorio, fecha, inicio, inicio.plusMinutes(MINUTOS_TURNO)));
            }
        }
        horarioRepository.saveAll(nuevos);
        return nuevos.size();
    }
}
