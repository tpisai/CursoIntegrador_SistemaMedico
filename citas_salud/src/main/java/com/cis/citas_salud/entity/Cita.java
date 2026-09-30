package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    public static final String RESERVADA = "RESERVADA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String ATENDIDA = "ATENDIDA";
    public static final String CANCELADA = "CANCELADA";
    public static final String NO_ASISTIO = "NO_ASISTIO";

    public static final List<String> ACTIVAS = List.of(RESERVADA, CONFIRMADA);
    public static final List<String> CERRADAS = List.of(ATENDIDA, NO_ASISTIO);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario", unique = true)
    private Horario horario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "motivo", columnDefinition = "text")
    private String motivo;

    @Column(name = "estado", nullable = false)
    private String estado = RESERVADA;

    @Column(name = "numero_ticket", nullable = false, unique = true)
    private String numeroTicket;

    public boolean estaActiva() {
        return ACTIVAS.contains(estado);
    }
}
