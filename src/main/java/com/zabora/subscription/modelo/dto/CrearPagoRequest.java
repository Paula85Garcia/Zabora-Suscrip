package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Builder
@Data
public class CrearPagoRequest {
    
    @NotNull(message = "El ID de suscripción es obligatorio")
    private String idSuscripcion;
    
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal monto;
    
    @NotNull(message = "El tipo de pago es obligatorio")
    private String tipoPago; // "tarjeta_credito" o "pse"
    
    private Boolean recibirFactura = false;
}