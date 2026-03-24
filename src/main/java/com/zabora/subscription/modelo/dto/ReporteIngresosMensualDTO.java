package com.zabora.subscription.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reporte de ingresos mensuales para administradores")
public class ReporteIngresosMensualDTO {
    
    @Schema(description = "Mes del reporte en formato YYYY-MM", example = "2026-03")
    private String mes;
    
    @Schema(description = "Total de pagos completados en el mes", example = "42")
    private Long totalPagos;
    
    @Schema(description = "Ingresos totales del mes en COP", example = "1255800.00")
    private BigDecimal ingresosTotales;
    
    @Schema(description = "Ingreso promedio por pago", example = "29900.00")
    private BigDecimal ingresoPromedio;
    
    @Schema(description = "Distribución de pagos por método")
    private Map<String, Long> pagosPorMetodo;  // "TARJETA_CREDITO": 35, "PSE": 7
    
    @Schema(description = "Cantidad de nuevas suscripciones en el mes", example = "38")
    private Long nuevasSuscripciones;
    
    @Schema(description = "Cantidad de cancelaciones en el mes", example = "4")
    private Long cancelaciones;
}