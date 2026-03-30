package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPagoBricksResponse {

    private String preferenceId;
    private String publicKey;
    private BigDecimal amount;
    private String currency;
    private String subscriptionId;
    private String paymentId;
}
