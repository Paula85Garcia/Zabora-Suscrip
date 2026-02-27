package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Respuesta de procesamiento de pago con Mercado Pago")
public class RespuestaPagoDTO {
    
    @Schema(description = "Indica si el pago fue aprobado", example = "true")
    private Boolean exito;
    
    @Schema(description = "Mensaje descriptivo del resultado", 
            example = "Pago aprobado correctamente")
    private String mensaje;
    
    @Schema(description = "ID del pago en Mercado Pago", example = "12345678901")
    private String idPago;
    
    @Schema(description = "Estado del pago según Mercado Pago", 
            example = "approved")
    private String estado;
    
    @Schema(description = "Monto pagado", example = "29900.00")
    private BigDecimal monto;
    
    @Schema(description = "Moneda del pago (ISO 4217)", example = "COP")
    private String moneda;
    
    @Schema(description = "Fecha de creación del pago")
    private LocalDateTime fechaPago;
    
    @Schema(description = "URL del comprobante o recurso externo de Mercado Pago", 
            example = "https://www.mercadopago.com.co/receipt/12345678901")
    private String urlComprobante;
    
    @Schema(description = "Indica si el pago está pendiente y requiere acción adicional (PSE, efectivo, etc.)", 
            example = "false")
    private Boolean requiereConfirmacion;
    
    @Schema(description = "Detalles adicionales devueltos por Mercado Pago")
    private Map<String, Object> detalles;

    @Schema(description = "Indica si el usuario solicitó recibir factura por email", 
            example = "true")
    private Boolean recibirFactura;
}