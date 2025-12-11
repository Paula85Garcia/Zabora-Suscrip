package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO (Data Transfer Object) que representa la respuesta de un procesamiento de pago.
 * Contiene información sobre el estado del pago, montos, comprobantes y posibles detalles adicionales.
 */
@Data
@Builder
@Schema(description = "Respuesta de procesamiento de pago")
public class RespuestaPagoDTO {

    
     // Indica si el pago fue exitoso o no.
     
    @Schema(description = "Indica si el pago fue exitoso", example = "true")
    private Boolean exito;

    // Mensaje descriptivo del resultado del pago.
    @Schema(description = "Mensaje descriptivo del resultado", 
            example = "Pago procesado exitosamente")
    private String mensaje;

    //Identificador único del pago.
    @Schema(description = "ID del pago", example = "pago_123456789")
    private String idPago;

     //Estado actual del pago (por ejemplo, PENDIENTE, COMPLETADO, FALLIDO).
    @Schema(description = "Estado del pago", example = "COMPLETADO")
    private String estado;

    //Monto del pago realizado.
    @Schema(description = "Monto pagado", example = "29900.00")
    private BigDecimal monto;

    //Moneda en la que se realizó el pago (por ejemplo, COP).
    @Schema(description = "Moneda del pago", example = "COP")
    private String moneda;

    //Fecha y hora en que se realizó el pago.
    @Schema(description = "Fecha del pago")
    private LocalDateTime fechaPago;

    //URL del comprobante del pago (PDF, página de recibo, etc.).
    @Schema(description = "URL del comprobante", 
            example = "https://receipt.stripe.com/test/123")
    private String urlComprobante;

    //Indica si se requiere confirmación adicional, por ejemplo en pagos PSE.
    @Schema(description = "Indica si requiere confirmación adicional (PSE)", 
            example = "false")
    private Boolean requiereConfirmacion;

    //Contiene detalles adicionales del pago, como metadatos o información extra de la pasarela.
    @Schema(description = "Detalles adicionales del pago")
    private Map<String, Object> detalles;
}
