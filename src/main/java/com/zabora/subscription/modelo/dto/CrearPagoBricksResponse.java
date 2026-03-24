package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para respuesta de creación de preferencia con Bricks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPagoBricksResponse {

    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private String publicKey;
    private BigDecimal amount;
    private String currency;
    private String subscriptionId;
    private String paymentId;
}