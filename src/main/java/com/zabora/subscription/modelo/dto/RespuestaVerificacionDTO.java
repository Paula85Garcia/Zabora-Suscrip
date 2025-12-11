package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO (Data Transfer Object) que representa la respuesta al verificar
 * el estado de una suscripción de usuario.
 * Contiene información sobre si la suscripción es válida, el plan actual,
 * su estado, fecha de expiración y límites del plan.
 */
@Data
@Builder
@Schema(description = "Respuesta de verificación de suscripción")
public class RespuestaVerificacionDTO {

    // Indica si la suscripción es válida (por ejemplo, si el usuario tiene un plan premium activo).
    @Schema(description = "Indica si la suscripción es válida (premium activa)", 
            example = "true")
    private Boolean valida;

    // Nombre del plan actual de la suscripción.
    @Schema(description = "Nombre del plan actual", example = "premium")
    private String plan;

    // Estado actual de la suscripción (ACTIVA, CANCELADA, EXPIRADA, PENDIENTE_PAGO, etc.).
    @Schema(description = "Estado de la suscripción", example = "ACTIVA")
    private String estado;

    // Fecha de expiración del periodo de suscripción actual.
    @Schema(description = "Fecha de expiración del periodo actual")
    private LocalDateTime fechaExpiracion;

    // Límites y características del plan actual en forma de clave-valor.
    @Schema(description = "Límites del plan actual")
    private Map<String, Object> limites;
}

