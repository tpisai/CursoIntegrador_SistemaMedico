package com.cis.citas_salud.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "consultorio")
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consultorio")
    private Integer idConsultorio;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "zona")
    private String zona;

    @Column(name = "piso")
    private String piso;

    @Column(name = "numero")
    private String numero;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    public Consultorio() {
    }

    public Consultorio(String nombre, String zona, String piso, String numero) {
        this.nombre = nombre;
        this.zona = zona;
        this.piso = piso;
        this.numero = numero;
    }

    /** Texto para mostrar, p. ej. "Módulo A · Consultorio 3". */
    public String getUbicacion() {
        return zona == null || zona.isBlank() ? nombre : zona + " · " + nombre;
    }

    public Integer getIdConsultorio() {
        return idConsultorio;
    }

    public void setIdConsultorio(Integer idConsultorio) {
        this.idConsultorio = idConsultorio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = piso;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
