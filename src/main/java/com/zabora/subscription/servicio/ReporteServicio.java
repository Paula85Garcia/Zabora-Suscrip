package com.zabora.subscription.servicio;

import com.zabora.subscription.repositorio.PagoRepository;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServicio {
    
    private final PagoRepository pagoRepository;
    private final UsuarioSuscripcionRepository suscripcionRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     *EXPLICACIÓN:
     * Es como el reporte que das mostrando cuánto dinero
     * ganó la empresa este mes.
     * 
     * Muestra:
     * - Total de ingresos
     * - Cantidad de pagos exitosos y fallidos
     * - Cuántos usuarios premium hay
     * - Tasa de conversión (gratuitos que se volvieron premium)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteMensual(int anio, int mes) {
        log.info("📊 Generando reporte mensual: {}-{}", anio, mes);
        
        YearMonth yearMonth = YearMonth.of(anio, mes);
        LocalDateTime fechaInicio = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime fechaFin = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        Map<String, Object> reporte = new HashMap<>();
        
        // 1. Ingresos totales del mes
        BigDecimal ingresosTotales = pagoRepository.calcularIngresosPorPeriodo(fechaInicio, fechaFin);
        reporte.put("ingresos_totales", ingresosTotales != null ? ingresosTotales : BigDecimal.ZERO);
        
        // 2. Cantidad de pagos por estado
        Long pagosExitosos = pagoRepository.contarPagosPorEstado(
            com.zabora.subscription.modelo.enumeracion.EstadoPago.COMPLETADO,
            fechaInicio, fechaFin
        );
        
        Long pagosFallidos = pagoRepository.contarPagosPorEstado(
            com.zabora.subscription.modelo.enumeracion.EstadoPago.FALLIDO,
            fechaInicio, fechaFin
        );
        
        reporte.put("pagos_exitosos", pagosExitosos != null ? pagosExitosos : 0L);
        reporte.put("pagos_fallidos", pagosFallidos != null ? pagosFallidos : 0L);
        
        Long totalPagos = (pagosExitosos != null ? pagosExitosos : 0L) + 
                         (pagosFallidos != null ? pagosFallidos : 0L);
        reporte.put("total_pagos", totalPagos);
        
        // 3. Suscripciones activas
        Long premiumActivos = suscripcionRepository.contarSuscripcionesPremiumActivas();
        reporte.put("suscripciones_premium_activas", premiumActivos != null ? premiumActivos : 0L);
        
        // 4. Calcular tasa de conversión (si hay datos)
        if (totalPagos > 0) {
            double tasaExito = (pagosExitosos.doubleValue() / totalPagos.doubleValue()) * 100;
            reporte.put("tasa_exito_pagos", String.format("%.2f%%", tasaExito));
        } else {
            reporte.put("tasa_exito_pagos", "0.00%");
        }
        
        // 5. Ingreso promedio por transacción
        if (pagosExitosos != null && pagosExitosos > 0 && ingresosTotales != null) {
            BigDecimal ingresoPromedio = ingresosTotales.divide(
                BigDecimal.valueOf(pagosExitosos),
                2,
                RoundingMode.HALF_UP
            );
            reporte.put("ingreso_promedio_transaccion", ingresoPromedio);
        } else {
            reporte.put("ingreso_promedio_transaccion", BigDecimal.ZERO);
        }
        
        // 6. Metadatos del reporte
        reporte.put("periodo", String.format("%d-%02d", anio, mes));
        reporte.put("fecha_generacion", LocalDateTime.now());
        reporte.put("generado_por", "sistema");
        
        log.info("✅ Reporte mensual generado para {}-{}", anio, mes);
        
        return reporte;
    }
    
    /**
     * 🎯 Reporte de ingresos por día (últimos 30 días)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reporteIngresosDiarios(int dias) {
        log.info("📊 Generando reporte de ingresos diarios (últimos {} días)", dias);
        
        String sql = """
            SELECT 
                DATE(p.fecha_pago) as fecha,
                COUNT(p.id) as cantidad_pagos,
                SUM(p.monto) as ingresos_dia,
                AVG(p.monto) as ingreso_promedio
            FROM pagos p
            WHERE p.estado = 'COMPLETADO'
              AND p.fecha_pago >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(p.fecha_pago)
            ORDER BY fecha DESC
        """;
        
        return jdbcTemplate.queryForList(sql, dias);
    }
    
    /**
     * 🎯 Estadísticas generales del negocio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> estadisticasGenerales() {
        log.info("📊 Generando estadísticas generales");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total de suscripciones por estado
        String sqlSuscripciones = """
            SELECT estado, COUNT(*) as cantidad
            FROM suscripciones_usuarios
            GROUP BY estado
        """;
        
        List<Map<String, Object>> suscripcionesPorEstado = jdbcTemplate.queryForList(sqlSuscripciones);
        stats.put("suscripciones_por_estado", suscripcionesPorEstado);
        
        // Total de ingresos históricos
        String sqlIngresos = """
            SELECT 
                SUM(monto) as ingresos_totales,
                COUNT(*) as total_transacciones
            FROM pagos
            WHERE estado = 'COMPLETADO'
        """;
        
        Map<String, Object> ingresos = jdbcTemplate.queryForMap(sqlIngresos);
        stats.put("ingresos_historicos", ingresos);
        
        // Métodos de pago más usados
        String sqlMetodos = """
            SELECT 
                metodo_pago,
                COUNT(*) as cantidad,
                SUM(monto) as ingresos
            FROM pagos
            WHERE estado = 'COMPLETADO'
            GROUP BY metodo_pago
        """;
        
        List<Map<String, Object>> metodosPago = jdbcTemplate.queryForList(sqlMetodos);
        stats.put("metodos_pago", metodosPago);
        
        // Suscripciones premium activas
        Long premiumActivos = suscripcionRepository.contarSuscripcionesPremiumActivas();
        stats.put("usuarios_premium_activos", premiumActivos);
        
        // Fecha de generación
        stats.put("fecha_generacion", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * 🎯 Suscripciones próximas a vencer (para notificaciones)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suscripcionesPorVencer(int dias) {
        log.info("⏰ Buscando suscripciones que vencen en {} días", dias);
        
        LocalDateTime fechaInicio = LocalDateTime.now();
        LocalDateTime fechaFin = fechaInicio.plusDays(dias);
        
        String sql = """
            SELECT 
                s.id as suscripcion_id,
                s.usuario_id,
                p.nombre as plan,
                s.fin_periodo_actual,
                DATEDIFF(s.fin_periodo_actual, NOW()) as dias_restantes
            FROM suscripciones_usuarios s
            JOIN planes_suscripcion p ON s.plan_id = p.id
            WHERE s.estado = 'ACTIVA'
              AND s.fin_periodo_actual BETWEEN ? AND ?
            ORDER BY s.fin_periodo_actual ASC
        """;
        
        return jdbcTemplate.queryForList(sql, fechaInicio, fechaFin);
    }
    
    /**
     * 🎯 Top usuarios por ingresos
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> topUsuariosPorIngresos(int limite) {
        log.info("🏆 Obteniendo top {} usuarios por ingresos", limite);
        
        String sql = """
            SELECT 
                p.usuario_id,
                COUNT(p.id) as cantidad_pagos,
                SUM(p.monto) as total_gastado,
                MAX(p.fecha_pago) as ultima_compra
            FROM pagos p
            WHERE p.estado = 'COMPLETADO'
            GROUP BY p.usuario_id
            ORDER BY total_gastado DESC
            LIMIT ?
        """;
        
        return jdbcTemplate.queryForList(sql, limite);
    }
}

