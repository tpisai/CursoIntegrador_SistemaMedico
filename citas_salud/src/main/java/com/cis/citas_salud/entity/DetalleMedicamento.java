package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_medicamento")
@Getter
@Setter
@NoArgsConstructor
public class DetalleMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atencion", nullable = false)
    private Atencion atencion;

    @Column(name = "nombre_medicamento", nullable = false, length = 200)
    private String nombreMedicamento;

    @Column(name = "dosis", length = 100)
    private String dosis;

    @Column(name = "cantidad", length = 50)
    private String cantidad;

    @Column(name = "via_administracion", length = 100)
    private String viaAdministracion;

    @Column(name = "presentacion", length = 100)
    private String presentacion;

    @Column(name = "unidades_por_toma", length = 50)
    private String unidadesPorToma;

    @Column(name = "frecuencia", length = 100)
    private String frecuencia;

    @Column(name = "duracion", length = 100)
    private String duracion;

    @Column(name = "orden")
    private Integer orden = 1;
}