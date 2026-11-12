package com.cis.citas_salud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resultado de un examen que reporta un servicio de apoyo al diagnóstico (laboratorio, ecografía, rayos X). */
@Entity
@Table(name = "resultado_examen")
@Getter
@Setter
@NoArgsConstructor
public class ResultadoExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Integer idResultado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atencion")
    private Atencion atencion;

    @Column(name = "tipo_examen", nullable = false)
    private String tipoExamen;

    @Column(name = "resultado", nullable = false)
    private String resultado;

    @Column(name = "valor_referencia")
    private String valorReferencia;

    @Column(name = "interpretacion", columnDefinition = "text")
    private String interpretacion;

    @Column(name = "orden", nullable = false)
    private Integer orden = 1;
}
