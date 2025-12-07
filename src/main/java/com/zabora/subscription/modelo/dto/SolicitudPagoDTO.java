package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Solicitud para procesar un pago")
public class SolicitudPagoDTO {
    
    @NotBlank(message = "El ID de suscripción es obligatorio")
    @Schema(description = "ID de la suscripción a pagar", 
            example = "sub_123456789", required = true)
    private String idSuscripcion;
    
    @NotNull(message = "El monto es obligatorio")
    @Schema(description = "Monto a pagar", example = "29900.00", required = true)
    private BigDecimal monto;
    
    @NotBlank(message = "El tipo de pago es obligatorio")
    @Schema(description = "Tipo de método de pago: 'tarjeta_credito' o 'pse'", 
            example = "tarjeta_credito", required = true)
    private String tipoPago;
    
    @Schema(description = "Token de tarjeta para pruebas", example = "tok_visa")
    private String tokenTarjetaPrueba;
    
    @Schema(description = "Detalles del método de pago PSE", example = "{\"banco\":\"Bancolombia\"}")
    private String detallesPse;
}
