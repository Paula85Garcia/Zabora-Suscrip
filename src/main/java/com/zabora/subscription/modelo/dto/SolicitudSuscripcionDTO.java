package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudSuscripcionDTO {

    private Integer usuarioId;

    @NotBlank(message = "El nombre del plan es obligatorio")
    private String nombrePlan;

    private String tipoPago;
}