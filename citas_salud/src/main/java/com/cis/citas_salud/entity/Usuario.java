package com.cis.citas_salud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "terminos_aceptados", nullable = false)
    private Boolean terminosAceptados = false;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /** "Ana Quispe": primer nombre y primer apellido. */
    public String getNombreCorto() {
        return primeraPalabra(nombres) + " " + primeraPalabra(apellidos);
    }

    private static String primeraPalabra(String texto) {
        return texto.trim().split("\\s+")[0];
    }
}
