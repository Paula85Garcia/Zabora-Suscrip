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
    
    private Boolean exito;
    private String mensaje;
    private String idSuscripcion;
    private String plan;
    private String estado;
    
    // NUEVOS CAMPOS
    @Schema(description = "Fecha de inicio del periodo actual")
    private LocalDateTime fechaInicio;
    
    @Schema(description = "Fecha de expiración del periodo actual")
    private LocalDateTime fechaExpiracion;
    
    @Schema(description = "Días restantes hasta la expiración")
    private Long diasRestantes;
    
    @Schema(description = "Horas restantes hasta la expiración")
    private Long horasRestantes;
    
    @Schema(description = "Indica si se cancelará al final del periodo")
    private Boolean cancelarAlFinalPeriodo;
    
    @Schema(description = "Fecha en que tomará efecto la cancelación")
    private LocalDateTime fechaEfectoCancelacion;
    
    @Schema(description = "Horas hasta que tome efecto la cancelación")
    private Long horasHastaCancelacion;
    
    private Map<String, Object> limites;
    private Boolean requierePago;
    private Map<String, Object> intentoPago;
    private Boolean elegibleReembolso;
    private LocalDateTime fechaCancelacion;
    
    //NUEVO: Opción de recibir factura
    @Schema(description = "Indica si el usuario quiere recibir factura por email")
    private Boolean recibirFactura;
}