package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Banner del carrusel de eventos que administración publica para los pacientes: nuevas
 * especialidades, ferias de salud, charlas, horarios especiales (sql/10_anuncios.sql).
 */
@Entity
@Table(name = "anuncio")
@Getter
@Setter
@NoArgsConstructor
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anuncio")
    private Integer idAnuncio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personal_administrativo")
    private PersonalAdministrativo personal;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "etiqueta", length = 40)
    private String etiqueta;

    @Column(name = "enlace")
    private String enlace;

    @Column(name = "texto_boton", length = 40)
    private String textoBoton;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "ruta_imagen", nullable = false, length = 500)
    private String rutaImagen;

    @Column(name = "tipo_mime", nullable = false, length = 50)
    private String tipoMime;

    @Column(name = "ancho_imagen", nullable = false)
    private Integer anchoImagen;

    @Column(name = "alto_imagen", nullable = false)
    private Integer altoImagen;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Lo ve el paciente: está activo y hoy cae dentro de su vigencia. */
    public boolean vigente(LocalDate hoy) {
        return activo && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }
}
