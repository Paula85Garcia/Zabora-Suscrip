package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPagoBricksRequest {

    @NotBlank(message = "El ID de suscripcion es requerido")
    private String idSuscripcion;

    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal monto;

    @NotBlank(message = "El tipo de pago es requerido")
    private String tipoPago;

    private Boolean recibirFactura = false;
}
