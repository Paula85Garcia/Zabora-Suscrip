package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSuscripcionDTO {

    private Boolean exito;
    private String mensaje;
    private String idSuscripcion;
    private String plan;
    private String estado;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaExpiracion;
    private Long diasRestantes;
    private Long horasRestantes;

    private Boolean cancelarAlFinalPeriodo;
    private LocalDateTime fechaEfectoCancelacion;
    private Long horasHastaCancelacion;

    private Boolean requierePago;
    private LocalDateTime fechaCancelacion;
}