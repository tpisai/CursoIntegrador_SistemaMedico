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

/** Código vigente para recuperar el PIN (RF-03): hay uno por usuario y se borra al usarlo. */
@Entity
@Table(name = "recuperacion_pin")
@Getter
@Setter
@NoArgsConstructor
public class RecuperacionPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recuperacion")
    private Integer idRecuperacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;
}
