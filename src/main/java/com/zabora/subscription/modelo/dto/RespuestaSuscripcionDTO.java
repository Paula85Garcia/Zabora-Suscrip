package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO (Data Transfer Object) que representa la respuesta de una operación relacionada
 * con la suscripción de un usuario, como creación, actualización o cancelación.
 * Contiene información sobre el estado de la operación, detalles del plan y pagos.
 */
@Data
@Builder
@Schema(description = "Respuesta de operación de suscripción")
public class RespuestaSuscripcionDTO {

    // Indica si la operación fue exitosa o no.
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private Boolean exito;

    // Mensaje descriptivo del resultado de la operación.
    @Schema(description = "Mensaje descriptivo del resultado", 
            example = "Suscripción creada exitosamente")
    private String mensaje;

    // Identificador único de la suscripción creada o modificada.
    @Schema(description = "ID de la suscripción creada o modificada", 
            example = "sub_123456789")
    private String idSuscripcion;

    // Nombre del plan asociado a la suscripción.
    @Schema(description = "Nombre del plan", example = "premium")
    private String plan;

    // Estado actual de la suscripción (por ejemplo, ACTIVA, CANCELADA, PENDIENTE_PAGO).
    @Schema(description = "Estado actual de la suscripción", example = "ACTIVA")
    private String estado;

    // Límites y características del plan en forma de clave-valor.
    @Schema(description = "Límites del plan")
    private Map<String, Object> limites;

    // Indica si se requiere un pago para activar la suscripción.
    @Schema(description = "Indica si requiere pago para activarse", example = "true")
    private Boolean requierePago;

    // Detalles del intento de pago relacionado con la suscripción, si aplica.
    @Schema(description = "Detalles del intento de pago (si aplica)")
    private Map<String, Object> intentoPago;

    // Indica si la suscripción es elegible para reembolso.
    @Schema(description = "Indica si es elegible para reembolso", example = "false")
    private Boolean elegibleReembolso;

    // Fecha en que la suscripción fue cancelada, si aplica.
    @Schema(description = "Fecha de cancelación (si aplica)")
    private LocalDateTime fechaCancelacion;
}
