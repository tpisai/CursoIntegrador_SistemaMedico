package com.cis.citas_salud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orden_examen")
@Getter
@Setter
@NoArgsConstructor
public class OrdenExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_examen")
    private Integer idOrdenExamen;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atencion", nullable = false, unique = true)
    private Atencion atencion;

    @Column(name = "examenes_requeridos", nullable = false, columnDefinition = "text")
    private String examenesRequeridos;

    @Column(name = "motivo_clinico", nullable = false, columnDefinition = "text")
    private String motivoClinico;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();
}
