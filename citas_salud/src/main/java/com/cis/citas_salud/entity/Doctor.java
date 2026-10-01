package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctor")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doctor")
    private Integer idDoctor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @Column(name = "cmp", nullable = false, unique = true)
    private String cmp;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    // Tabla doctor_consultorio (máximo 2 doctores por consultorio, se valida en el servicio).
    @ManyToMany
    @JoinTable(
            name = "doctor_consultorio",
            joinColumns = @JoinColumn(name = "id_doctor"),
            inverseJoinColumns = @JoinColumn(name = "id_consultorio"))
    @OrderBy("idConsultorio")
    private List<Consultorio> consultorios = new ArrayList<>();
}
