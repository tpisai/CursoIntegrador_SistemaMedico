package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_medico")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atencion")
    private Atencion atencion;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();
}
