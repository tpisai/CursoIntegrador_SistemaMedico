package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla notificacion (sql/citas_salud.sql). */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    // Valores permitidos por chk_notificacion_tipo.
    public static final String CITA = "CITA";
    public static final String SOLICITUD = "SOLICITUD";
    public static final String CAMPANIA = "CAMPANIA";
    public static final String SISTEMA = "SISTEMA";
    public static final String RECORDATORIO = "RECORDATORIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();
}
