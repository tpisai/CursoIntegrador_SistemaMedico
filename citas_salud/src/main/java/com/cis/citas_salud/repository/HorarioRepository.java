package com.cis.citas_salud.repository;

import com.cis.citas_salud.entity.Horario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    /** Bloquea la fila mientras se reserva, para que dos pacientes no tomen el mismo cupo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Horario h where h.idHorario = :id")
    Optional<Horario> bloquear(@Param("id") Integer idHorario);

    List<Horario> findByDoctor_IdDoctorAndConsultorio_IdConsultorioAndFechaAndEstadoNotOrderByHoraInicio(
            Integer idDoctor, Integer idConsultorio, LocalDate fecha, String estado);

    List<Horario> findByDoctor_IdDoctorAndConsultorio_IdConsultorioAndFechaBetweenAndEstado(
            Integer idDoctor, Integer idConsultorio, LocalDate desde, LocalDate hasta, String estado);

    List<Horario> findByDoctor_IdDoctorAndFechaBetweenAndEstadoNot(
            Integer idDoctor, LocalDate desde, LocalDate hasta, String estado);

    @EntityGraph(attributePaths = "consultorio")
    List<Horario> findByDoctor_IdDoctorAndFechaBetweenAndEstadoOrderByFechaAscHoraInicioAsc(
            Integer idDoctor, LocalDate desde, LocalDate hasta, String estado);
}
