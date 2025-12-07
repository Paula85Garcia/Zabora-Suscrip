package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Respuesta de verificación de suscripción")
public class RespuestaVerificacionDTO {
    
    @Schema(description = "Indica si la suscripción es válida (premium activa)", 
            example = "true")
    private Boolean valida;
    
    @Schema(description = "Nombre del plan actual", example = "premium")
    private String plan;
    
    @Schema(description = "Estado de la suscripción", example = "ACTIVA")
    private String estado;
    
    @Schema(description = "Fecha de expiración del periodo actual")
    private LocalDateTime fechaExpiracion;
    
    @Schema(description = "Límites del plan actual")
    private Map<String, Object> limites;
}
