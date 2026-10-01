package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "atencion")
@Getter
@Setter
@NoArgsConstructor
public class Atencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atencion")
    private Integer idAtencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historia")
    private HistoriaClinica historia;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doctor")
    private Doctor doctor;

    @Column(name = "fecha_atencion", nullable = false)
    private LocalDateTime fechaAtencion = LocalDateTime.now();

    @Column(name = "estado", nullable = false)
    private String estado = "ABIERTA";

    @Column(name = "anamnesis", columnDefinition = "text")
    private String anamnesis;

    @Column(name = "examen_fisico", columnDefinition = "text")
    private String examenFisico;

    @Column(name = "diagnostico", columnDefinition = "text")
    private String diagnostico;

    @Column(name = "tratamiento", columnDefinition = "text")
    private String tratamiento;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;
}
