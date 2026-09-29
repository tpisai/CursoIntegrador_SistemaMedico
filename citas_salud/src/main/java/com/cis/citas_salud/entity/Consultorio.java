package com.cis.citas_salud.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "consultorio")
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consultorio")
    private Integer idConsultorio;

    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "piso")
    private Integer piso;

    public Consultorio() {
    }

    public Consultorio(String numero, Integer piso) {
        this.numero = numero;
        this.piso = piso;
    }

    public Integer getIdConsultorio() {
        return idConsultorio;
    }

    public void setIdConsultorio(Integer idConsultorio) {
        this.idConsultorio = idConsultorio;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Integer getPiso() {
        return piso;
    }

    public void setPiso(Integer piso) {
        this.piso = piso;
    }
}