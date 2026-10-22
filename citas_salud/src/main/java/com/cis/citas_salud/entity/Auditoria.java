package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla auditoria (sql/citas_salud.sql): acciones registradas para seguimiento y control. */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    // Acción que registra recepción al validar el ticket de una cita (RF-12).
    public static final String VALIDAR_TICKET = "VALIDAR_TICKET";
    public static final String ENTIDAD_CITA = "cita";

    // Valores permitidos por chk_auditoria_resultado.
    public static final String EXITOSO = "EXITOSO";
    public static final String FALLIDO = "FALLIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "entidad", nullable = false)
    private String entidad;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "id_registro")
    private Integer idRegistro;

    @Column(name = "resultado", nullable = false)
    private String resultado = EXITOSO;

    @Column(name = "ip")
    private String ip;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Auditoria(Usuario usuario, String accion, String entidad, Integer idRegistro, String descripcion) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.idRegistro = idRegistro;
        this.descripcion = descripcion;
    }
}
