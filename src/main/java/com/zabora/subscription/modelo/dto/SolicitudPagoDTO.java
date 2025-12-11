package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO (Data Transfer Object) que representa la solicitud de un pago.
 * Contiene información necesaria para procesar un pago asociado a una suscripción.
 */
@Data
@Schema(description = "Solicitud para procesar un pago")
public class SolicitudPagoDTO {

    // ID de la suscripción que se desea pagar. Obligatorio.
    @NotBlank(message = "El ID de suscripción es obligatorio")
    @Schema(description = "ID de la suscripción a pagar", 
            example = "sub_123456789", required = true)
    private String idSuscripcion;

    // Monto que se desea pagar. Obligatorio.
    @NotNull(message = "El monto es obligatorio")
    @Schema(description = "Monto a pagar", example = "29900.00", required = true)
    private BigDecimal monto;

    // Tipo de método de pago: puede ser 'tarjeta_credito' o 'pse'. Obligatorio.
    @NotBlank(message = "El tipo de pago es obligatorio")
    @Schema(description = "Tipo de método de pago: 'tarjeta_credito' o 'pse'", 
            example = "tarjeta_credito", required = true)
    private String tipoPago;

    // Token de tarjeta para pruebas en entornos de sandbox.
    @Schema(description = "Token de tarjeta para pruebas", example = "tok_visa")
    private String tokenTarjetaPrueba;

    // Información adicional para pagos PSE, como el banco o la referencia.
    @Schema(description = "Detalles del método de pago PSE", example = "{\"banco\":\"Bancolombia\"}")
    private String detallesPse;
}

