package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO (Data Transfer Object) que representa la solicitud para crear una nueva suscripción.
 * Contiene información sobre el plan, el método de pago y datos necesarios para pruebas.
 */
@Data
@Schema(description = "Solicitud para crear una suscripción")
public class SolicitudSuscripcionDTO {

    // Nombre del plan que se desea contratar. Obligatorio.
    @NotBlank(message = "El nombre del plan es obligatorio")
    @Schema(description = "Nombre del plan: 'gratuito' o 'premium'", 
            example = "premium", required = true)
    private String nombrePlan;

    // ID del método de pago en Stripe. Opcional, útil en entornos de prueba.
    @Schema(description = "ID del método de pago en Stripe (opcional para pruebas)", 
            example = "pm_1PABCDEFGHIJK")
    private String idMetodoPago;

    // Tipo de método de pago utilizado: 'tarjeta_credito' o 'pse'. Obligatorio.
    @NotNull(message = "El tipo de pago es obligatorio")
    @Schema(description = "Tipo de método de pago: 'tarjeta_credito' o 'pse'", 
            example = "tarjeta_credito", required = true)
    private String tipoPago;

    // Token de tarjeta para pruebas. NO debe usarse en producción.
    @Schema(description = "Token de tarjeta para pruebas (NO usar en producción)", 
            example = "tok_visa")
    private String tokenTarjetaPrueba;
}

