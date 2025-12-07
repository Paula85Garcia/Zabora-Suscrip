package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Respuesta de procesamiento de pago")
public class RespuestaPagoDTO {
    
    @Schema(description = "Indica si el pago fue exitoso", example = "true")
    private Boolean exito;
    
    @Schema(description = "Mensaje descriptivo del resultado", 
            example = "Pago procesado exitosamente")
    private String mensaje;
    
    @Schema(description = "ID del pago", example = "pago_123456789")
    private String idPago;
    
    @Schema(description = "Estado del pago", example = "COMPLETADO")
    private String estado;
    
    @Schema(description = "Monto pagado", example = "29900.00")
    private BigDecimal monto;
    
    @Schema(description = "Moneda del pago", example = "COP")
    private String moneda;
    
    @Schema(description = "Fecha del pago")
    private LocalDateTime fechaPago;
    
    @Schema(description = "URL del comprobante", 
            example = "https://receipt.stripe.com/test/123")
    private String urlComprobante;
    
    @Schema(description = "Indica si requiere confirmación adicional (PSE)", 
            example = "false")
    private Boolean requiereConfirmacion;
    
    @Schema(description = "Detalles adicionales del pago")
    private Map<String, Object> detalles;
}