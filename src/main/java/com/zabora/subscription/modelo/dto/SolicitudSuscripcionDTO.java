package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudSuscripcionDTO {
    
    @NotBlank(message = "El nombre del plan es obligatorio")
    private String nombrePlan;
    
    @NotNull(message = "El tipo de pago es obligatorio")
    private String tipoPago;
}