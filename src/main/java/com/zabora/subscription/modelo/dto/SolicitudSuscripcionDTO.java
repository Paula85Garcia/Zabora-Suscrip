package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Solicitud para crear una suscripción")
public class SolicitudSuscripcionDTO {
    
    @NotBlank(message = "El nombre del plan es obligatorio")
    @Schema(description = "Nombre del plan: 'gratuito' o 'premium'", 
            example = "premium", required = true)
    private String nombrePlan;
    
    @Schema(description = "ID del método de pago en Stripe (opcional para pruebas)", 
            example = "pm_1PABCDEFGHIJK")
    private String idMetodoPago;
    
    @NotNull(message = "El tipo de pago es obligatorio")
    @Schema(description = "Tipo de método de pago: 'tarjeta_credito' o 'pse'", 
            example = "tarjeta_credito", required = true)
    private String tipoPago;
    
    @Schema(description = "Token de tarjeta para pruebas (NO usar en producción)", 
            example = "tok_visa")
    private String tokenTarjetaPrueba;
}
