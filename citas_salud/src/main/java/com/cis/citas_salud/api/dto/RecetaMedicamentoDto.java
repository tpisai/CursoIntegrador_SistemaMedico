package com.cis.citas_salud.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecetaMedicamentoDto {

    private String nombreMedicamento;
    private String dosis;
    private String cantidad;
    private String viaAdministracion;
    private String presentacion;
    private String unidadesPorToma;
    private String frecuencia;
    private String duracion;
}