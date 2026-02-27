package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta después de crear una preferencia de pago en MercadoPago
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPagoResponse {
    
    // Datos de MercadoPago
    private String preferenceId;        // ID de la preferencia creada
    private String initPoint;           // URL para checkout en producción
    private String sandboxInitPoint;    // URL para checkout en sandbox/testing
    private String publicKey;           // Public key de MercadoPago
    
    // Datos del pago
    private BigDecimal amount;
    private String currency;
    
    // Datos internos
    private String subscriptionId;
    private String paymentId;
}