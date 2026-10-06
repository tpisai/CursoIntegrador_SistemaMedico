package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "personal_administrativo")
@Getter
@Setter
@NoArgsConstructor
public class PersonalAdministrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personal_administrativo")
    private Integer idPersonalAdministrativo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    // Tabla cargo (catálogo); por ahora solo se guarda su id.
    @Column(name = "id_cargo", nullable = false)
    private Integer idCargo;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";
}
