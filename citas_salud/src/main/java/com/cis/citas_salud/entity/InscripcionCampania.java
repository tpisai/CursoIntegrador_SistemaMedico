package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Inscripción de un paciente a una campaña (RF-21). */
@Entity
@Table(name = "inscripcion_campania")
@Getter
@Setter
@NoArgsConstructor
public class InscripcionCampania {

    public static final String INSCRITO = "INSCRITO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Integer idInscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_campania")
    private Campania campania;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion = LocalDateTime.now();

    @Column(name = "estado", nullable = false)
    private String estado = INSCRITO;

    /** Código del comprobante digital, p. ej. "C-000012". */
    public String getCodigo() {
        return "C-" + String.format("%06d", idInscripcion);
    }
}
