package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "horario")
@Getter
@Setter
@NoArgsConstructor
public class Horario {

    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String RESERVADO = "RESERVADO";
    public static final String CANCELADO = "CANCELADO";
    public static final String FINALIZADO = "FINALIZADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer idHorario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doctor")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_consultorio")
    private Consultorio consultorio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "estado", nullable = false)
    private String estado = DISPONIBLE;

    public Horario(Doctor doctor, Consultorio consultorio, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        this.doctor = doctor;
        this.consultorio = consultorio;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public boolean yaPaso(LocalDateTime ahora) {
        return !LocalDateTime.of(fecha, horaInicio).isAfter(ahora);
    }

    /** Libre para reservar: disponible y todavía no empezó. */
    public boolean estaLibre(LocalDateTime ahora) {
        return DISPONIBLE.equals(estado) && !yaPaso(ahora);
    }
}
