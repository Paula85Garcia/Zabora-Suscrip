package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ErrorDTO {
    private LocalDateTime timestamp;
    private String mensaje;
    private String detalle;
    private String ruta;
    private String codigoError;
}