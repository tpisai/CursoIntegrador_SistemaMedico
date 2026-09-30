package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "historia_clinica")
@Getter
@Setter
@NoArgsConstructor
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historia")
    private Integer idHistoria;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", unique = true)
    private Paciente paciente;

    @Column(name = "numero_historia", nullable = false, unique = true)
    private String numeroHistoria;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura = LocalDate.now();

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVA";

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;
}
