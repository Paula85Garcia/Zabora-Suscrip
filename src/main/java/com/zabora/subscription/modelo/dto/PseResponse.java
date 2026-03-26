package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PseResponse {
    private boolean exito;
    private String pagoId;
    private String redirectUrl;
    private String estado;
    private String mensaje;
}
