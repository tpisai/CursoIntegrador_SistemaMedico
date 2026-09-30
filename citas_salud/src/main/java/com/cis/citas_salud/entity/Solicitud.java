package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Cambio de horario o derivación que un doctor pide a administración (RF-15 a RF-18). */
@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    public static final String CAMBIO_HORARIO = "CAMBIO_HORARIO";
    public static final String DERIVACION = "DERIVACION";
    public static final String PENDIENTE = "PENDIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doctor_solicitante")
    private Doctor doctor;

    // Personal administrativo que revisa (su panel llega en una siguiente entrega).
    @Column(name = "id_personal_revisor")
    private Integer idPersonalRevisor;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "motivo", nullable = false, columnDefinition = "text")
    private String motivo;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "estado", nullable = false)
    private String estado = PENDIENTE;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "respuesta_admin", columnDefinition = "text")
    private String respuestaAdmin;

    // JSON con los datos propios de cada tipo (nuevo horario, hospital destino, etc.).
    @Column(name = "datos_adicionales", columnDefinition = "text")
    private String datosAdicionales;
}
