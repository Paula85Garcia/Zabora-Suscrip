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
    @Schema(description = "ID de la suscripción a pagar")
    private String idSuscripcion;
    
    @NotNull(message = "El monto es obligatorio")
    @Schema(description = "Monto a pagar")
    private BigDecimal monto;
    
    @NotBlank(message = "El tipo de pago es obligatorio")
    @Schema(description = "Tipo de método de pago: 'tarjeta_credito' o 'pse'")
    private String tipoPago;
    
    @Schema(description = "Token de tarjeta para pruebas")
    private String tokenTarjetaPrueba;
    
    @Schema(description = "Detalles del método de pago PSE")
    private String detallesPse;
    
    // NUEVO
    @Schema(description = "Indica si el usuario quiere recibir factura por email")
    private Boolean recibirFactura;
}