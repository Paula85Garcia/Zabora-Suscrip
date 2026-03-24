package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear preferencia de pago con MercadoPago Checkout Bricks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPagoBricksRequest {

    @NotBlank(message = "ID de suscripción es requerido")
    private String idSuscripcion;

    @NotNull(message = "Monto es requerido")
    @Positive(message = "Monto debe ser positivo")
    private BigDecimal monto;

    @NotBlank(message = "Tipo de pago es requerido")
    private String tipoPago; // "tarjeta_credito", "pse", "wallet"

    private Boolean recibirFactura = false;

    private String descripcion;
}