package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Respuesta de operación de suscripción")
public class RespuestaSuscripcionDTO {
    
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private Boolean exito;
    
    @Schema(description = "Mensaje descriptivo del resultado", 
            example = "Suscripción creada exitosamente")
    private String mensaje;
    
    @Schema(description = "ID de la suscripción creada o modificada", 
            example = "sub_123456789")
    private String idSuscripcion;
    
    @Schema(description = "Nombre del plan", example = "premium")
    private String plan;
    
    @Schema(description = "Estado actual de la suscripción", example = "ACTIVA")
    private String estado;
    
    @Schema(description = "Límites del plan")
    private Map<String, Object> limites;
    
    @Schema(description = "Indica si requiere pago para activarse", example = "true")
    private Boolean requierePago;
    
    @Schema(description = "Detalles del intento de pago (si aplica)")
    private Map<String, Object> intentoPago;
    
    @Schema(description = "Indica si es elegible para reembolso", example = "false")
    private Boolean elegibleReembolso;
    
    @Schema(description = "Fecha de cancelación (si aplica)")
    private LocalDateTime fechaCancelacion;
}