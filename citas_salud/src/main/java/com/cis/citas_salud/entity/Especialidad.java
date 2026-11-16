package com.cis.citas_salud.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidad")
public class Especialidad {

    public static final String CONSULTA = "CONSULTA";
    /** Laboratorio, ecografía y rayos X: no hacen consulta médica, reportan resultados de exámenes. */
    public static final String APOYO_DIAGNOSTICO = "APOYO_DIAGNOSTICO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "tipo", nullable = false)
    private String tipo = CONSULTA;

    public Especialidad() {
    }

    public Especialidad(String nombre) {
        this.nombre = nombre;
    }

    public Integer getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(Integer idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean esApoyoDiagnostico() {
        return APOYO_DIAGNOSTICO.equals(tipo);
    }
}
