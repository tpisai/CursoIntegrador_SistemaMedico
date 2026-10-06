package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Campaña médica publicada por administración (RF-19, RF-20). */
@Entity
@Table(name = "campania")
@Getter
@Setter
@NoArgsConstructor
public class Campania {

    public static final String PLANIFICADA = "PLANIFICADA";
    public static final String ACTIVA = "ACTIVA";
    public static final List<String> VIGENTES = List.of(PLANIFICADA, ACTIVA);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_campania")
    private Integer idCampania;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personal_administrativo")
    private PersonalAdministrativo personal;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "lugar")
    private String lugar;

    @Column(name = "estado", nullable = false)
    private String estado = PLANIFICADA;

    @Column(name = "imagen")
    private String imagen;

    @Column(name = "cupos", nullable = false)
    private Integer cupos;

    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;
}
